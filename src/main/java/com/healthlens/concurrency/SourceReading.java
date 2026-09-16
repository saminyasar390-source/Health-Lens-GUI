package com.healthlens.concurrency;

/**
 * What one data source came back with. This is the V in
 * {@code Callable<V>} / {@code Future<V>} — the reason the sync tasks are
 * Callables rather than Runnables: a Runnable can only run, it cannot hand
 * a value back, and it cannot report a checked failure.
 *
 * LAB CONCEPTS: Callable&lt;V&gt; returning a value, Future&lt;V&gt;
 * carrying success / failure / cancellation (Lab 2, Task 9).
 */
public class SourceReading {

    public enum Status {
        OK, FAILED, TIMED_OUT, CANCELLED
    }

    private final String source;
    private final Status status;
    private final String message;

    private final Double sleepHours;
    private final Double waterGlasses;
    private final Double exerciseMinutes;
    private final Double stressLevel;

    private final long durationMillis;

    private SourceReading(String source,
                          Status status,
                          String message,
                          Double sleepHours,
                          Double waterGlasses,
                          Double exerciseMinutes,
                          Double stressLevel,
                          long durationMillis) {
        this.source = source;
        this.status = status;
        this.message = message;
        this.sleepHours = sleepHours;
        this.waterGlasses = waterGlasses;
        this.exerciseMinutes = exerciseMinutes;
        this.stressLevel = stressLevel;
        this.durationMillis = durationMillis;
    }

    public static SourceReading ok(String source,
                                   Double sleepHours,
                                   Double waterGlasses,
                                   Double exerciseMinutes,
                                   Double stressLevel,
                                   long durationMillis) {
        return new SourceReading(source, Status.OK, "updated",
                sleepHours, waterGlasses, exerciseMinutes, stressLevel, durationMillis);
    }

    public static SourceReading failed(String source, String message) {
        return new SourceReading(source, Status.FAILED, message, null, null, null, null, 0L);
    }

    public static SourceReading timedOut(String source) {
        return new SourceReading(source, Status.TIMED_OUT, "took too long - skipped",
                null, null, null, null, 0L);
    }

    public static SourceReading cancelled(String source) {
        return new SourceReading(source, Status.CANCELLED, "cancelled", null, null, null, null, 0L);
    }

    public String getSource() {
        return source;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Double getSleepHours() {
        return sleepHours;
    }

    public Double getWaterGlasses() {
        return waterGlasses;
    }

    public Double getExerciseMinutes() {
        return exerciseMinutes;
    }

    public Double getStressLevel() {
        return stressLevel;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isOk() {
        return status == Status.OK;
    }
}
