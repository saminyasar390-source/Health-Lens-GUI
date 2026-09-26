package com.healthlens.model;
public class WaterMetric extends HealthMetric {
    public WaterMetric(){super("Water");}
    @Override public double calculateScore(HealthData d){return SleepMetric.clamp(d.getWaterGlasses()/d.getWaterGoalGlasses());}
}
