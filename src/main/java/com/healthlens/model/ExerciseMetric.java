package com.healthlens.model;
public class ExerciseMetric extends HealthMetric {
    public ExerciseMetric(){super("Exercise");}
    @Override public double calculateScore(HealthData d){return SleepMetric.clamp(d.getExerciseMinutes()/d.getExerciseGoalMinutes());}
}
