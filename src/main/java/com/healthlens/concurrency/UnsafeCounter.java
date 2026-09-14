package com.healthlens.concurrency;

/**
 * From Lab 2, Task 5: a shared mutable counter with NO synchronization.
 * count++ is not atomic (read, increment, write are three separate steps),
 * so when two threads increment this at the same time, updates get lost.
 */
public class UnsafeCounter {
    int count = 0;

    void increment() {
        count++;
    }
}
