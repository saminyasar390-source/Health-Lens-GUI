package com.healthlens.concurrency;

/**
 * From Lab 2, Task 7: a one-slot shared buffer. put() waits while the slot
 * is full; take() waits while it's empty. Both use the same monitor (this),
 * and both recheck their condition in a while loop after waking up, since a
 * notify doesn't guarantee the condition is still true when this thread
 * finally gets the monitor back.
 *
 * Generic so the exact same class can hold Integers for the lab demo AND
 * Strings for HealthLens's real health-tip notification pipeline — one
 * correct, reusable implementation of the pattern, not two copies.
 */
public class Box<T> {
    private T item;
    private boolean hasItem;

    public synchronized void put(T value) throws InterruptedException {
        while (hasItem) {
            wait();
        }
        item = value;
        hasItem = true;
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (!hasItem) {
            wait();
        }
        T value = item;
        hasItem = false;
        notifyAll();
        return value;
    }

    public synchronized boolean hasItem() {
        return hasItem;
    }

    public synchronized T peek() {
        return item;
    }
}
