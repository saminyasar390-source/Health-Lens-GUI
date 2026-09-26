package com.healthlens.model;
public class SleepMetric extends HealthMetric {
    public SleepMetric(){super("Sleep");}
    @Override public double calculateScore(HealthData d){return clamp(d.getSleepHours()/d.getSleepGoalHours());}
    protected static double clamp(double v){return Math.max(0,Math.min(1,v));}
}
