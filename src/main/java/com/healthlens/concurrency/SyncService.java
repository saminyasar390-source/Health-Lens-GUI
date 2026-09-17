package com.healthlens.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Pulls today's numbers from every connected data source AT THE SAME TIME
 * instead of one after another, and merges whatever comes back.
 *
 * WHY A POOL AND NOT FOUR RAW THREADS (Lab 2, Task 8.2): the user can press
 * Sync as often as they like. Creating four fresh platform threads on every
 * press would be pure waste — thread creation costs memory and scheduling
 * time, and nothing would stop a impatient user from spawning dozens. A
 * fixed pool of three reuses the same workers forever and makes the fourth
 * task simply queue up behind them.
 *
 * WHAT MAKES THIS PRODUCTION-SHAPED RATHER THAN A DEMO:
 *   - each source is a Callable, so it returns a real value or reports a
 *     real failure instead of writing to a field and hoping;
 *   - each Future is collected with a TIMEOUT, so one dead source cannot
 *     hang the whole sync forever;
 *   - a source that times out is cancelled with cancel(true), which
 *     interrupts it - and because the tasks sleep, they notice;
 *   - the user can cancel the whole sync mid-flight;
 *   - shutdown follows the exact shutdown/awaitTermination/shutdownNow
 *     pattern from the lab manual, so the JVM can actually exit.
 *
 * LAB CONCEPTS: ExecutorService, fixed thread pool, submit(), Callable&lt;V&gt;,
 * Future&lt;V&gt;, get() with timeout, cancel(true), graceful shutdown
 * (Lab 2, Tasks 8, 9 and 10).
 */
public class SyncService {

    public static final String POOL_ACTIVITY_ID = "sync-pool";
    public static final String COORDINATOR_ACTIVITY_ID = "sync-coordinator";

    /** Called from the coordinator thread — wrap UI work in Platform.runLater. */
    public interface Listener {
        void onSyncStarted(List<String> sourceNames);

        void onSourceFinished(SourceReading reading);

        void onSyncFinished(MergedReading merged, List<SourceReading> readings);
    }

    private static final int POOL_SIZE = 3;
    private static final long PER_SOURCE_TIMEOUT_SECONDS = 4;

    private final Random random = new Random();
    private final java.util.concurrent.atomic.AtomicInteger workerNumber =
            new java.util.concurrent.atomic.AtomicInteger(1);

    private ExecutorService pool;
    private Thread coordinator;

    private final List<Future<SourceReading>> inFlight = new CopyOnWriteArrayList<>();
    private final Map<String, SourceReading> lastReadings = new ConcurrentHashMap<>();

    private volatile boolean syncing;

    public boolean isSyncing() {
        return syncing;
    }

    public Map<String, SourceReading> getLastReadings() {
        return lastReadings;
    }

    public List<String> getSourceNames() {
        List<String> names = new ArrayList<>();
        names.add("Smartwatch");
        names.add("Phone pedometer");
        names.add("Water log");
        names.add("Clinic records");
        return names;
    }

    /**
     * Kicks off one full sync. Returns immediately; everything else happens
     * on the pool and on a coordinator thread, so the UI never freezes.
     */
    public synchronized boolean syncAll(Listener listener) {
        if (syncing) {
            return false;
        }
        syncing = true;

        if (pool == null || pool.isShutdown()) {
            // Naming the pool's threads is not cosmetic: it is what makes
            // the Activity Center readable and what makes a thread dump
            // usable when something goes wrong in production.
            pool = Executors.newFixedThreadPool(POOL_SIZE, runnable -> {
                Thread t = new Thread(runnable, "sync-worker-" + workerNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            });
            ActivityRegistry.getInstance().register(new ActivityInfo(
                    POOL_ACTIVITY_ID,
                    "Sync pool (" + POOL_SIZE + " workers)",
                    "Fetches from all your connected sources at once, reusing the same workers.",
                    "ExecutorService + fixed thread pool, tasks queue when workers are busy",
                    null,
                    "idle"));
        }

        inFlight.clear();
        final MergedReading merged = new MergedReading();
        final List<String> names = getSourceNames();

        // Every worker merges its own result into the SAME object, which is
        // why MergedReading has to be synchronized.
        submit("Smartwatch", merged, () -> {
            long started = System.currentTimeMillis();
            Thread.sleep(700 + random.nextInt(500));
            return SourceReading.ok("Smartwatch",
                    5 + random.nextDouble() * 4,
                    null,
                    null,
                    2 + random.nextDouble() * 6,
                    System.currentTimeMillis() - started);
        });

        submit("Phone pedometer", merged, () -> {
            long started = System.currentTimeMillis();
            Thread.sleep(400 + random.nextInt(400));
            return SourceReading.ok("Phone pedometer",
                    null,
                    null,
                    random.nextDouble() * 35,
                    1 + random.nextDouble() * 5,
                    System.currentTimeMillis() - started);
        });

        submit("Water log", merged, () -> {
            long started = System.currentTimeMillis();
            Thread.sleep(200 + random.nextInt(300));
            return SourceReading.ok("Water log",
                    null,
                    2 + random.nextDouble() * 4,
                    null,
                    null,
                    System.currentTimeMillis() - started);
        });

        // Deliberately unreliable, the way a real third-party endpoint is.
        submit("Clinic records", merged, () -> {
            long started = System.currentTimeMillis();
            if (random.nextInt(4) == 0) {
                Thread.sleep(10_000); // will hit the timeout and be cancelled
            }
            Thread.sleep(600 + random.nextInt(400));
            return SourceReading.ok("Clinic records",
                    null,
                    1 + random.nextDouble() * 2,
                    null,
                    null,
                    System.currentTimeMillis() - started);
        });

        ActivityRegistry.getInstance().updateStatus(POOL_ACTIVITY_ID,
                "running " + names.size() + " fetches on " + POOL_SIZE + " workers");

        coordinator = new Thread(() -> collectResults(names, merged, listener), "sync-coordinator");
        coordinator.setDaemon(true);
        coordinator.start();

        ActivityRegistry.getInstance().register(new ActivityInfo(
                COORDINATOR_ACTIVITY_ID,
                "Sync coordinator",
                "Waits for every source, drops the slow ones, and applies the merged result.",
                "Future.get(timeout), Future.cancel(true), blocking safely off the FX thread",
                coordinator,
                "collecting results"));

        listener.onSyncStarted(names);
        return true;
    }

    private void submit(String name, MergedReading merged, Callable<SourceReading> work) {
        inFlight.add(pool.submit(() -> {
            SourceReading reading = work.call();
            merged.merge(reading); // shared mutable state, touched by 4 threads
            return reading;
        }));
    }

    /**
     * Runs on the coordinator thread. Calling Future.get() here BLOCKS,
     * which is fine and correct — this is not the JavaFX thread. Doing the
     * same thing in a button handler is what freezes GUIs.
     */
    private void collectResults(List<String> names, MergedReading merged, Listener listener) {
        List<SourceReading> readings = new ArrayList<>();
        try {
            for (int i = 0; i < inFlight.size(); i++) {
                String name = i < names.size() ? names.get(i) : "Source " + (i + 1);
                Future<SourceReading> future = inFlight.get(i);
                SourceReading reading;
                try {
                    reading = future.get(PER_SOURCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true); // interrupt the stuck worker
                    reading = SourceReading.timedOut(name);
                } catch (CancellationException e) {
                    reading = SourceReading.cancelled(name);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    reading = SourceReading.failed(name,
                            cause == null ? "unknown error" : String.valueOf(cause.getMessage()));
                }
                readings.add(reading);
                lastReadings.put(name, reading);
                listener.onSourceFinished(reading);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            for (Future<SourceReading> future : inFlight) {
                future.cancel(true);
            }
        } finally {
            syncing = false;
            ActivityRegistry.getInstance().updateStatus(POOL_ACTIVITY_ID, "idle");
            ActivityRegistry.getInstance().unregister(COORDINATOR_ACTIVITY_ID);
            listener.onSyncFinished(merged, readings);
        }
    }

    /** User pressed Cancel. Requests interruption; tasks that sleep will notice. */
    public void cancelSync() {
        for (Future<SourceReading> future : inFlight) {
            future.cancel(true);
        }
        if (coordinator != null) {
            coordinator.interrupt();
        }
    }

    /**
     * The graceful shutdown pattern straight out of the lab manual: ask
     * politely, wait, then insist. Without this the pool's worker threads
     * would keep the JVM alive after the window closes.
     */
    public void shutdown() {
        if (pool == null || pool.isShutdown()) {
            return;
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.err.println("[HealthLens] sync pool did not terminate.");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            ActivityRegistry.getInstance().unregister(POOL_ACTIVITY_ID);
            ActivityRegistry.getInstance().unregister(COORDINATOR_ACTIVITY_ID);
        }
    }
}
