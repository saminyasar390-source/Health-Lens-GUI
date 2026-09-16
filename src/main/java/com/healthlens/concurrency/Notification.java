package com.healthlens.concurrency;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * One reminder produced by {@link ReminderService} and consumed by
 * {@link NotificationCenterService}.
 *
 * Deliberately IMMUTABLE apart from the read flag. An immutable object
 * handed between two threads needs no synchronization at all to stay
 * consistent — the safest kind of shared data there is. The one mutable
 * field is volatile so a change made on one thread is visible on the other.
 *
 * LAB CONCEPTS: visibility (Lab 2, Task 5.3), safe publication.
 */
public class Notification {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final String category;
    private final String text;
    private final LocalTime createdAt;

    private volatile boolean read;

    public Notification(String category, String text) {
        this.category = category;
        this.text = text;
        this.createdAt = LocalTime.now();
    }

    public String getCategory() {
        return category;
    }

    public String getText() {
        return text;
    }

    public String getTimeText() {
        return createdAt.format(TIME_FORMAT);
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }

    @Override
    public String toString() {
        return getTimeText() + "  " + text;
    }
}
