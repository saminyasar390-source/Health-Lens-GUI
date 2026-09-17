package com.healthlens.concurrency;

/**
 * From Lab 2, Task 6: the same counter, protected with synchronized methods.
 * Both methods share the same monitor (this), so only one thread can be
 * inside increment()/getCount() at a time — the lost-update problem from
 * UnsafeCounter goes away.
 */
public class SafeCounter {
    private int count;

    synchronized void increment() {
        count++;
    }

    synchronized int getCount() {
        return count;
    }
}
