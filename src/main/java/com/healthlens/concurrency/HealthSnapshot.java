package com.healthlens.concurrency;

/**
 * An immutable copy of "where the user stands right now" — today's entries
 * plus the goals they're measured against.
 *
 * WHY THIS CLASS EXISTS: the reminder thread needs to know whether the user
 * is behind on water before it decides to nag them. It must NOT reach into
 * the JavaFX sliders to find out, because reading live UI controls off the
 * FX thread is exactly the kind of unsafe shared access Lab 2 Task 5 warns
 * about. Instead the FX thread builds one of these (all fields final, so it
 * can never change afterwards) and publishes it through a single volatile
 * reference. The background thread reads that reference and gets a
 * consistent set of values that cannot be half-updated underneath it.
 *
 * LAB CONCEPTS: atomicity of a compound read, visibility, safe publication
 * of immutable state (Lab 2, Task 5.3).
 */
public final class HealthSnapshot {

    public final double sleepHours;
    public final double waterGlasses;
    public final double exerciseMinutes;
    public final double stressLevel;

    public final double sleepGoalHours;
    public final double waterGoalGlasses;
    public final double exerciseGoalMinutes;
    public final double stressComfortMax;

    public HealthSnapshot(double sleepHours,
                          double waterGlasses,
                          double exerciseMinutes,
                          double stressLevel,
                          double sleepGoalHours,
                          double waterGoalGlasses,
                          double exerciseGoalMinutes,
                          double stressComfortMax) {
        this.sleepHours = sleepHours;
        this.waterGlasses = waterGlasses;
        this.exerciseMinutes = exerciseMinutes;
        this.stressLevel = stressLevel;
        this.sleepGoalHours = sleepGoalHours;
        this.waterGoalGlasses = waterGoalGlasses;
        this.exerciseGoalMinutes = exerciseGoalMinutes;
        this.stressComfortMax = stressComfortMax;
    }

    public boolean isBehindOnWater() {
        return waterGlasses < waterGoalGlasses;
    }

    public boolean isBehindOnExercise() {
        return exerciseMinutes < exerciseGoalMinutes;
    }

    public boolean isStressed() {
        return stressLevel > stressComfortMax;
    }

    public boolean isBehindOnSleep() {
        return sleepHours < sleepGoalHours;
    }

    public boolean allGoalsMet() {
        return !isBehindOnWater() && !isBehindOnExercise() && !isStressed() && !isBehindOnSleep();
    }
}
