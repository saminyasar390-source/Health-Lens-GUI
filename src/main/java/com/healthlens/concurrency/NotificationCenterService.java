package com.healthlens.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * THE CONSUMER half of the reminder pipeline, plus the inbox history that
 * the Activity Center displays.
 *
 * One dedicated thread sits blocked on {@link NotificationQueue#take()}.
 * Because take() uses wait() rather than polling, this thread sits in
 * WAITING and uses no CPU at all until a reminder actually arrives — open
 * the Activity Center and you can watch it do exactly that.
 *
 * IMPORTANT FOR CALLERS: listeners are invoked ON THIS BACKGROUND THREAD.
 * Any listener that touches JavaFX controls must wrap its work in
 * Platform.runLater(...). That rule is the single most important one in a
 * threaded GUI, and it is why this class has no JavaFX imports whatsoever.
 *
 * LAB CONCEPTS: consumer half of producer-consumer (Task 7), wait()-based
 * blocking instead of busy waiting (Task 7.1), cooperative interruption
 * (Task 4), atomic variables and concurrent collections (Task 8).
 */
public class NotificationCenterService {

    public static final String ACTIVITY_ID = "notification-consumer";

    /** Implemented by the dashboard and the Activity Center. Called off the FX thread. */
    public interface Listener {
        void onNotification(Notification notification);
    }

    private static final int HISTORY_LIMIT = 50;

    private final NotificationQueue<Notification> queue;

    /** Several threads add/remove listeners; iteration must never break. */
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private final List<Notification> history = new CopyOnWriteArrayList<>();

    /** Incremented on the consumer thread, read and reset on the FX thread. */
    private final AtomicInteger unreadCount = new AtomicInteger();

    private Thread consumerThread;

    public NotificationCenterService(NotificationQueue<Notification> queue) {
        this.queue = queue;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (consumerThread != null && consumerThread.isAlive()) {
            return;
        }

        consumerThread = new Thread(this::consumeLoop, "notification-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        ActivityRegistry.getInstance().register(new ActivityInfo(
                ACTIVITY_ID,
                "Notification centre",
                "Sleeps until a reminder is ready, then shows it. Costs nothing while idle.",
                "Consumer half of producer-consumer, blocked in wait() inside take()",
                consumerThread,
                "idle - inbox empty"));
    }

    private void consumeLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Notification notification = queue.take(); // WAITING until something arrives

                ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID, "delivering a reminder");

                history.add(notification);
                trimHistory();
                unreadCount.incrementAndGet();

                for (Listener listener : listeners) {
                    try {
                        listener.onNotification(notification);
                    } catch (RuntimeException ex) {
                        // One misbehaving listener must not kill the pipeline.
                        System.err.println("[HealthLens] notification listener failed: " + ex.getMessage());
                    }
                }

                ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID, "idle - inbox empty");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID, "stopped");
        }
    }

    private void trimHistory() {
        while (history.size() > HISTORY_LIMIT) {
            history.remove(0);
        }
    }

    /** Newest first, so the Activity Center can show it top-down. */
    public List<Notification> getHistoryNewestFirst() {
        List<Notification> copy = new ArrayList<>(history);
        java.util.Collections.reverse(copy);
        return copy;
    }

    public int getUnreadCount() {
        return unreadCount.get();
    }

    public void markAllRead() {
        for (Notification n : history) {
            n.markRead();
        }
        unreadCount.set(0);
    }

    public void stop() {
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    public void awaitStop(long millis) {
        if (consumerThread == null) {
            return;
        }
        try {
            consumerThread.join(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
