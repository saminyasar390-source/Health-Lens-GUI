package com.healthlens.model;

/**
 * A functional interface: exactly one abstract method, so any block of code
 * that takes a normalized 0..1 metric score and returns a boolean can stand
 * in for a ScoreRule. Used in ScoreCalculator once as an anonymous class and
 * once as a lambda, to show both ways of implementing the same interface
 * (Lab 1 Foundations, Task 10).
 */
@FunctionalInterface
public interface ScoreRule {
    boolean isHealthy(double normalizedScore);
}
