package com.healthlens.concurrency;

/**
 * One object that owns every background service in HealthLens, so the rest
 * of the app never has to think about threads.
 *
 * The dashboard creates this once, calls {@link #start()}, and calls
 * {@link #shutdown()} when the window closes. That single ownership point
 * is what stops the classic "the window is gone but the JVM won't exit"
 * bug: there is exactly one place where every thread and every executor is
 * asked to stop, and it cannot be forgotten in one branch of the code.
 *
 * Note the shutdown ORDER. The producer is stopped first so no new
 * reminders can appear, then the consumer, then the pool. Stopping them the
 * other way round could leave a producer blocked in put() on a queue nobody
 * will ever drain again.
 *
 * LAB CONCEPTS: executor lifecycle vs task coordination (Task 9.3),
 * cooperative interruption, join() with a bounded wait.
 */
public class BackgroundServices {

    /** Small enough that stale nudges can't stack up while you're away. */
    private static final int INBOX_CAPACITY = 5;

    /** How often the reminder thread re-checks your goals. */
    private static final long REMINDER_INTERVAL_MILLIS = 60_000L;

    private static final long PHASE_MILLIS = 4_000L;

    private final NotificationQueue<Notification> inbox = new NotificationQueue<>(INBOX_CAPACITY);
    private final ReminderService reminderService = new ReminderService(inbox, REMINDER_INTERVAL_MILLIS);
    private final NotificationCenterService notificationCenter = new NotificationCenterService(inbox);
    private final SyncService syncService = new SyncService();
    private final BreathingSession breathingSession = new BreathingSession(PHASE_MILLIS);

    private volatile boolean started;

    public void start() {
        if (started) {
            return;
        }
        started = true;
        // Consumer first, so it is already waiting when the first reminder
        // is produced.
        notificationCenter.start();
        reminderService.start();
    }

    public NotificationQueue<Notification> getInbox() {
        return inbox;
    }

    public ReminderService getReminderService() {
        return reminderService;
    }

    public NotificationCenterService getNotificationCenter() {
        return notificationCenter;
    }

    public SyncService getSyncService() {
        return syncService;
    }

    public BreathingSession getBreathingSession() {
        return breathingSession;
    }

    public ActivityRegistry getActivityRegistry() {
        return ActivityRegistry.getInstance();
    }

    /** Called exactly once, from the dashboard's window close handler. */
    public void shutdown() {
        reminderService.stop();
        breathingSession.stop();

        // Wake the consumer even if it is parked in wait() on an empty inbox.
        notificationCenter.stop();

        reminderService.awaitStop(1_000L);
        notificationCenter.awaitStop(1_000L);
        breathingSession.awaitFinish(1_000L);

        syncService.cancelSync();
        syncService.shutdown();

        ActivityRegistry.getInstance().clear();
        started = false;
    }
}
