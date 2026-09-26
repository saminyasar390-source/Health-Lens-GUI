package com.healthlens.model;
public class StressMetric extends HealthMetric {
    public StressMetric(){super("Stress");}
    @Override public double calculateScore(HealthData d){return SleepMetric.clamp(1.0-((d.getStressLevel()-1.0)/9.0));}
}
