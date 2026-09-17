package com.healthlens.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * THE PRODUCER half of HealthLens's reminder pipeline.
 *
 * One dedicated background thread wakes up every so often, looks at the
 * latest {@link HealthSnapshot} the dashboard published, decides whether the
 * user actually needs a nudge, and if so puts one reminder into the shared
 * {@link NotificationQueue}. It does not touch the UI at all.
 *
 * What makes this practical rather than a demo:
 *   - reminders are chosen from real data (behind on water -> water nudge),
 *     so the user never gets told to drink water when they already hit 8;
 *   - the same category never fires twice in a row;
 *   - the user can switch reminders off, and the thread stays alive but
 *     quiet rather than being killed and recreated;
 *   - put() blocks when the inbox is full, so an idle app cannot build up
 *     a backlog of stale nudges.
 *
 * LAB CONCEPTS: Runnable task separated from its Thread (Task 3), sleep()
 * (Task 4), cooperative interruption with the flag restored (Task 4),
 * producer side of producer-consumer (Task 7), volatile for cross-thread
 * visibility of the snapshot and the enabled flag (Task 5).
 */
public class ReminderService {

    public static final String ACTIVITY_ID = "reminder-producer";

    private static final long FIRST_CHECK_MILLIS = 12_000L;

    private final NotificationQueue<Notification> queue;
    private final long checkIntervalMillis;

    private Thread producerThread;

    /** Published by the FX thread, read by the producer thread. */
    private volatile HealthSnapshot snapshot;
    private volatile boolean enabled = true;

    private String lastCategory = "";

    public ReminderService(NotificationQueue<Notification> queue, long checkIntervalMillis) {
        this.queue = queue;
        this.checkIntervalMillis = checkIntervalMillis;
    }

    /** Called from the JavaFX thread every time the dashboard is updated. */
    public void publishSnapshot(HealthSnapshot latest) {
        this.snapshot = latest;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID,
                enabled ? "watching your goals" : "paused by you");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void start() {
        if (producerThread != null && producerThread.isAlive()) {
            return;
        }

        // Runnable = the work. Thread = one way of running that work.
        Runnable task = this::produceLoop;

        producerThread = new Thread(task, "reminder-producer");
        producerThread.setDaemon(true);
        producerThread.start();

        ActivityRegistry.getInstance().register(new ActivityInfo(
                ACTIVITY_ID,
                "Reminder watcher",
                "Checks your goals every minute and queues a nudge when you're falling behind.",
                "Runnable + Thread, sleep(), producer half of producer-consumer",
                producerThread,
                "watching your goals"));
    }

    private void produceLoop() {
        try {
            // The first check comes sooner than the rest: a user who opens
            // the app already behind on water shouldn't wait a full minute
            // to hear about it.
            long nextWait = FIRST_CHECK_MILLIS;

            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(nextWait);
                nextWait = checkIntervalMillis;

                if (!enabled) {
                    continue;
                }

                HealthSnapshot current = snapshot; // one volatile read of an immutable object
                if (current == null) {
                    continue;
                }

                Notification due = decideReminder(current);
                if (due != null) {
                    ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID,
                            queue.isFull() ? "inbox full - holding back" : "queueing a reminder");

                    // Blocks while the inbox is full. This is deliberate
                    // back-pressure, not a bug.
                    queue.put(due);

                    lastCategory = due.getCategory();
                    ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID, "watching your goals");
                }
            }
        } catch (InterruptedException e) {
            // Restore the flag so anything above this level still sees the
            // cancellation request, then exit cleanly.
            Thread.currentThread().interrupt();
        } finally {
            ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID, "stopped");
        }
    }

    /**
     * Picks the most relevant nudge, or null when the user needs nothing.
     *
     * Every goal the user is currently behind on becomes a candidate, then
     * the first one that isn't a repeat of last time wins. If only one
     * thing is wrong, it does repeat rather than going silent - going
     * silent would be worse than repeating.
     */
    private Notification decideReminder(HealthSnapshot s) {
        List<Notification> candidates = new ArrayList<>();

        if (s.isBehindOnWater()) {
            int shortfall = (int) Math.ceil(s.waterGoalGlasses - s.waterGlasses);
            candidates.add(new Notification("water",
                    "\uD83D\uDCA7 " + shortfall + " more glass" + (shortfall == 1 ? "" : "es")
                            + " to hit your water goal."));
        }
        if (s.isStressed()) {
            candidates.add(new Notification("stress",
                    "\uD83E\uDDD8 Stress is sitting at " + Math.round(s.stressLevel)
                            + ". A one-minute breathing break would help."));
        }
        if (s.isBehindOnExercise()) {
            int shortfall = (int) Math.ceil(s.exerciseGoalMinutes - s.exerciseMinutes);
            candidates.add(new Notification("exercise",
                    "\uD83D\uDEB6 " + shortfall + " more active minutes to reach today's movement goal."));
        }
        if (s.isBehindOnSleep()) {
            candidates.add(new Notification("sleep",
                    "\uD83C\uDF19 You logged " + String.format(Locale.US, "%.1f", s.sleepHours)
                            + "h of sleep. Try to get closer to " + Math.round(s.sleepGoalHours) + "h tonight."));
        }

        if (candidates.isEmpty()) {
            // Everything is met. Say so once, then stay quiet.
            if ("celebrate".equals(lastCategory)) {
                return null;
            }
            return new Notification("celebrate", "\u2705 Every goal met today. Nicely done.");
        }

        for (Notification candidate : candidates) {
            if (!candidate.getCategory().equals(lastCategory)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    /** Cooperative shutdown — asks, doesn't force. */
    public void stop() {
        if (producerThread != null) {
            producerThread.interrupt();
        }
    }

    /** Waits for the producer to finish, using join() (Lab 2, Task 4). */
    public void awaitStop(long millis) {
        if (producerThread == null) {
            return;
        }
        try {
            producerThread.join(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
