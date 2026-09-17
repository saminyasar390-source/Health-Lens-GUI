package com.healthlens.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single place that knows what background work HealthLens is doing
 * right now. Services register themselves here when they start and update
 * their status as they go; the Activity Center window reads a snapshot
 * every half second to draw its list.
 *
 * WHY THIS IS THREAD-SAFE THE WAY IT IS:
 * registrations arrive from several different threads (the reminder
 * producer, the notifier, the sync coordinator, the breathing thread) while
 * the JavaFX thread reads them. A {@link ConcurrentHashMap} handles that
 * without any locking on our side, and a synchronized list keeps a stable
 * display order.
 *
 * LAB CONCEPTS: concurrent collections from java.util.concurrent (Lab 2,
 * Task 8), shared mutable state accessed from multiple threads (Task 5).
 */
public final class ActivityRegistry {

    private static final ActivityRegistry INSTANCE = new ActivityRegistry();

    public static ActivityRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<String, ActivityInfo> activities = new ConcurrentHashMap<>();
    private final List<String> displayOrder = Collections.synchronizedList(new ArrayList<String>());

    private ActivityRegistry() {
        // singleton
    }

    public void register(ActivityInfo info) {
        if (activities.put(info.getId(), info) == null) {
            displayOrder.add(info.getId());
        }
    }

    public void updateStatus(String id, String status) {
        ActivityInfo info = activities.get(id);
        if (info != null) {
            info.setStatus(status);
        }
    }

    public void unregister(String id) {
        activities.remove(id);
        displayOrder.remove(id);
    }

    /** A point-in-time copy, safe to iterate on the JavaFX thread. */
    public List<ActivityInfo> snapshot() {
        List<ActivityInfo> out = new ArrayList<>();
        synchronized (displayOrder) {
            for (String id : displayOrder) {
                ActivityInfo info = activities.get(id);
                if (info != null) {
                    out.add(info);
                }
            }
        }
        return out;
    }

    public void clear() {
        activities.clear();
        displayOrder.clear();
    }
}
