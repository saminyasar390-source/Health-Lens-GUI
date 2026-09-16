package com.healthlens.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * A data-integrity self-test, tucked away under Diagnostics.
 *
 * Real apps ship self-tests like this. The point here is honest: it proves,
 * on the user's own machine, that HealthLens's shared counters are guarded
 * properly, by running the same workload twice — once through an
 * unprotected tally and once through a guarded one — and showing that only
 * the guarded one is trustworthy.
 *
 * TWO DETAILS THAT MAKE THE RACE ACTUALLY SHOW UP:
 *   1. A CountDownLatch start gate holds every thread at the line until
 *      they can all be released at once. Without it the first thread often
 *      finishes before the last one starts, and the race quietly hides.
 *   2. A second latch is awaited before reading the results. Reaching zero
 *      on a latch establishes a happens-before relationship, so the values
 *      read afterwards are guaranteed to be the final ones — otherwise the
 *      test itself would have a visibility bug.
 *
 * LAB CONCEPTS: race condition and lost updates, count++ not being atomic,
 * synchronized methods as the fix, CountDownLatch as a synchronizer,
 * visibility guarantees (Lab 2, Tasks 5, 6 and 8).
 */
public final class IntegrityCheck {

    private IntegrityCheck() {
    }

    /** What the self-test found. */
    public static final class Result {
        public final int expected;
        public final int unprotectedTotal;
        public final int guardedTotal;
        public final long millis;

        public Result(int expected, int unprotectedTotal, int guardedTotal, long millis) {
            this.expected = expected;
            this.unprotectedTotal = unprotectedTotal;
            this.guardedTotal = guardedTotal;
            this.millis = millis;
        }

        public int lostUpdates() {
            return expected - unprotectedTotal;
        }

        public boolean guardedIsCorrect() {
            return guardedTotal == expected;
        }
    }

    /** No synchronization at all. count++ is read, add, write - three steps. */
    private static final class PlainTally {
        int value;

        void increment() {
            value++;
        }
    }

    /** Same field, same workload, one keyword's difference. */
    private static final class GuardedTally {
        private int value;

        synchronized void increment() {
            value++;
        }

        synchronized int get() {
            return value;
        }
    }

    /**
     * Runs the check. BLOCKS until finished, so callers must run it on a
     * background thread, never on the JavaFX thread.
     */
    public static Result run(int threadCount, int incrementsPerThread) throws InterruptedException {
        long started = System.currentTimeMillis();

        PlainTally plain = new PlainTally();
        GuardedTally guarded = new GuardedTally();

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(threadCount * 2);

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            threads.add(new Thread(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        plain.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            }, "integrity-unprotected-" + (i + 1)));

            threads.add(new Thread(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        guarded.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            }, "integrity-guarded-" + (i + 1)));
        }

        for (Thread t : threads) {
            t.setDaemon(true);
            t.start();
        }

        startGate.countDown();   // release them all at the same instant
        finishGate.await();      // and only read once every one has finished

        return new Result(
                threadCount * incrementsPerThread,
                plain.value,
                guarded.get(),
                System.currentTimeMillis() - started);
    }
}
