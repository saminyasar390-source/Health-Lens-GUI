package com.healthlens.concurrency;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The reminder inbox: a BOUNDED buffer shared by exactly two threads — the
 * reminder producer and the notifier consumer.
 *
 * This is the one-slot Box from Lab 2 Task 7, grown up into something the
 * app genuinely needs. Making it bounded rather than unbounded is the whole
 * point: if the user walks away from their desk, put() blocks once the inbox
 * is full instead of piling up 400 stale "drink water" reminders to dump on
 * them when they come back. That back-pressure IS the feature.
 *
 * THE THREE RULES THIS CLASS FOLLOWS (Lab 2, Task 7):
 *   1. wait()/notifyAll() are only ever called while holding this object's
 *      own monitor — every method here is synchronized.
 *   2. Every guarded condition is rechecked in a WHILE loop, never an if,
 *      because waking up does not prove the condition became true.
 *   3. notifyAll() happens AFTER the state change, so a woken thread
 *      rechecks against the new state.
 *
 * LAB CONCEPTS: intrinsic monitors, synchronized methods, wait(),
 * notifyAll(), guarded conditions, producer-consumer coordination.
 */
public class NotificationQueue<T> {

    private final int capacity;
    private final Deque<T> items = new ArrayDeque<>();

    public NotificationQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.capacity = capacity;
    }

    /**
     * Blocking put, for the producer thread. Waits while the inbox is full.
     * NEVER call this from the JavaFX thread — it would freeze the UI.
     */
    public synchronized void put(T item) throws InterruptedException {
        while (items.size() == capacity) {
            wait();
        }
        items.addLast(item);
        notifyAll();
    }

    /**
     * Non-blocking put, safe to call from the JavaFX thread (used when the
     * user taps "remind me later"). Returns false instead of waiting if the
     * inbox is already full.
     */
    public synchronized boolean offer(T item) {
        if (items.size() == capacity) {
            return false;
        }
        items.addLast(item);
        notifyAll();
        return true;
    }

    /**
     * Blocking take, for the notifier thread. Waits while the inbox is
     * empty — this is why the notifier sits in WAITING almost all the time
     * and costs essentially nothing, instead of busy-spinning.
     */
    public synchronized T take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        T value = items.removeFirst();
        notifyAll();
        return value;
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized int getCapacity() {
        return capacity;
    }

    public synchronized boolean isFull() {
        return items.size() == capacity;
    }

    public synchronized void clear() {
        items.clear();
        notifyAll();
    }
}
