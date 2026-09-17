package com.healthlens.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * THE REAL SHARED MUTABLE OBJECT IN THIS APP.
 *
 * During a sync, three worker threads run at the same time and each writes
 * its result into this ONE object. Two of them can overlap on the same
 * field: the smartwatch reports a stress level derived from heart rate, and
 * the phone reports a stress level derived from screen time, and whichever
 * lands last should win cleanly rather than half-win. Reading and writing
 * the "best available" value is a compound read-modify-write, exactly the
 * count++ problem from Lab 2 Task 5 — just wearing different clothes.
 *
 * HOW IT'S PROTECTED:
 *   - one PRIVATE FINAL lock object guards every field, so no outside code
 *     can accidentally synchronize on something else and think it's safe
 *     (the "two different lock objects protect nothing" trap from Task 6);
 *   - every read and every write goes through that same monitor, which is
 *     what gives both atomicity AND visibility;
 *   - sourcesReported is an AtomicInteger because it's only ever a simple
 *     counter and needs no surrounding critical section.
 *
 * LAB CONCEPTS: shared resource, critical section, race condition,
 * synchronized blocks on a private final lock, the monitor rule,
 * AtomicInteger (Lab 2, Tasks 5, 6 and 8).
 */
public class MergedReading {

    private final Object lock = new Object();

    private Double sleepHours;
    private Double waterGlasses;
    private Double exerciseMinutes;
    private Double stressLevel;

    /** How many source values were actually folded in. */
    private final AtomicInteger sourcesReported = new AtomicInteger();

    /**
     * Folds one source's result into the shared totals. Called concurrently
     * by every sync worker, so the whole body is one critical section.
     */
    public void merge(SourceReading reading) {
        if (reading == null || !reading.isOk()) {
            return;
        }
        synchronized (lock) {
            if (reading.getSleepHours() != null) {
                sleepHours = reading.getSleepHours();
            }
            if (reading.getWaterGlasses() != null) {
                // Water is cumulative across sources, not overwritten:
                // a genuine read-modify-write that must not be interleaved.
                waterGlasses = (waterGlasses == null ? 0.0 : waterGlasses) + reading.getWaterGlasses();
            }
            if (reading.getExerciseMinutes() != null) {
                exerciseMinutes = (exerciseMinutes == null ? 0.0 : exerciseMinutes) + reading.getExerciseMinutes();
            }
            if (reading.getStressLevel() != null) {
                // Keep the higher of the two stress readings - the more
                // cautious one - which is again read-then-write.
                stressLevel = (stressLevel == null)
                        ? reading.getStressLevel()
                        : Math.max(stressLevel, reading.getStressLevel());
            }
        }
        sourcesReported.incrementAndGet();
    }

    public Double getSleepHours() {
        synchronized (lock) {
            return sleepHours;
        }
    }

    public Double getWaterGlasses() {
        synchronized (lock) {
            return waterGlasses;
        }
    }

    public Double getExerciseMinutes() {
        synchronized (lock) {
            return exerciseMinutes;
        }
    }

    public Double getStressLevel() {
        synchronized (lock) {
            return stressLevel;
        }
    }

    public int getSourcesReported() {
        return sourcesReported.get();
    }
}
