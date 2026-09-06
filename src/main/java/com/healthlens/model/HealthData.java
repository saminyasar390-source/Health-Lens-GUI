package com.healthlens.model;

/**
 * MODEL: plain data holder for a day's health entries and the goals they're
 * measured against. Deliberately has zero JavaFX imports — it doesn't know
 * a GUI exists, so it can be reused, unit-tested, or serialized on its own.
 */
public class HealthData {

    // Default daily entries (used on first launch and by "Reset")
    public static final double DEFAULT_SLEEP_HOURS = 7.0;
    public static final double DEFAULT_WATER_GLASSES = 6.0;
    public static final double DEFAULT_EXERCISE_MINUTES = 20.0;
    public static final double DEFAULT_STRESS_LEVEL = 5.0;
    public static final String DEFAULT_MOOD = "Okay";

    // Default goals (used on first launch, and restored by "Reset goals" if you add one later)
    public static final double DEFAULT_SLEEP_GOAL_HOURS = 8.0;
    public static final double DEFAULT_WATER_GOAL_GLASSES = 8.0;
    public static final double DEFAULT_EXERCISE_GOAL_MINUTES = 30.0;
    public static final double DEFAULT_STRESS_COMFORT_MAX = 4.0;

    private double sleepHours = DEFAULT_SLEEP_HOURS;
    private double waterGlasses = DEFAULT_WATER_GLASSES;
    private double exerciseMinutes = DEFAULT_EXERCISE_MINUTES;
    private double stressLevel = DEFAULT_STRESS_LEVEL;
    private String mood = DEFAULT_MOOD;

    private double sleepGoalHours = DEFAULT_SLEEP_GOAL_HOURS;
    private double waterGoalGlasses = DEFAULT_WATER_GOAL_GLASSES;
    private double exerciseGoalMinutes = DEFAULT_EXERCISE_GOAL_MINUTES;
    private double stressComfortMax = DEFAULT_STRESS_COMFORT_MAX;

    // ----- today's entries -----

    public double getSleepHours() { return sleepHours; }
    public void setSleepHours(double sleepHours) { this.sleepHours = sleepHours; }

    public double getWaterGlasses() { return waterGlasses; }
    public void setWaterGlasses(double waterGlasses) { this.waterGlasses = waterGlasses; }

    public double getExerciseMinutes() { return exerciseMinutes; }
    public void setExerciseMinutes(double exerciseMinutes) { this.exerciseMinutes = exerciseMinutes; }

    public double getStressLevel() { return stressLevel; }
    public void setStressLevel(double stressLevel) { this.stressLevel = stressLevel; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    // ----- goals -----

    public double getSleepGoalHours() { return sleepGoalHours; }
    public void setSleepGoalHours(double sleepGoalHours) { this.sleepGoalHours = sleepGoalHours; }

    public double getWaterGoalGlasses() { return waterGoalGlasses; }
    public void setWaterGoalGlasses(double waterGoalGlasses) { this.waterGoalGlasses = waterGoalGlasses; }

    public double getExerciseGoalMinutes() { return exerciseGoalMinutes; }
    public void setExerciseGoalMinutes(double exerciseGoalMinutes) { this.exerciseGoalMinutes = exerciseGoalMinutes; }

    public double getStressComfortMax() { return stressComfortMax; }
    public void setStressComfortMax(double stressComfortMax) { this.stressComfortMax = stressComfortMax; }

    /** Puts today's entries (not the goals) back to their defaults. Used by the Reset button. */
    public void resetEntriesToDefaults() {
        sleepHours = DEFAULT_SLEEP_HOURS;
        waterGlasses = DEFAULT_WATER_GLASSES;
        exerciseMinutes = DEFAULT_EXERCISE_MINUTES;
        stressLevel = DEFAULT_STRESS_LEVEL;
        mood = DEFAULT_MOOD;
    }
}
