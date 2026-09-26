package com.healthlens.model;

/** Abstract base class demonstrating abstraction and polymorphism for health metrics. */
public abstract class HealthMetric {
    private final String name;
    protected HealthMetric(String name){this.name=name;}
    public String getName(){return name;}
    /** Returns a normalized score between 0 and 1. */
    public abstract double calculateScore(HealthData data);
}
