package com.healthlens.model;

/**
 * MODEL (service): all the scoring/business rules live here, and nowhere
 * else. No JavaFX imports, no UI references — this class could be unit
 * tested with plain JUnit and would never need a GUI to run.
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
        // static-only utility class
    }

    /** Scores a HealthData snapshot and builds the human-readable summary text. */
    public static ScoreResult calculate(HealthData data) {
        // Polymorphism: all concrete metrics are treated through the abstract HealthMetric type.
        HealthMetric[] metrics = { new SleepMetric(), new WaterMetric(), new ExerciseMetric(), new StressMetric() };
        double sleepScore = metrics[0].calculateScore(data);
        double waterScore = metrics[1].calculateScore(data);
        double exerciseScore = metrics[2].calculateScore(data);
        double stressScore = metrics[3].calculateScore(data);
        double overall = (sleepScore + waterScore + exerciseScore + stressScore) / metrics.length;

        String tier;
        if (overall >= 0.8) tier = "good";
        else if (overall >= 0.5) tier = "medium";
        else tier = "poor";

        // ScoreRule is a user-defined functional interface (one abstract method).
        // Anonymous class implementation: an explicit method body, no target-type inference.
        ScoreRule strictRule = new ScoreRule() {
            @Override
            public boolean isHealthy(double normalizedScore) {
                return normalizedScore >= 0.7;
            }
        };
        // Lambda implementation of the exact same interface: concise, compiler infers the type.
        ScoreRule lenientRule = normalizedScore -> normalizedScore >= 0.4;

        int metricsNeedingAttention = 0;
        for (double metricScore : new double[] { sleepScore, waterScore, exerciseScore, stressScore }) {
            if (!lenientRule.isHealthy(metricScore)) {
                metricsNeedingAttention++;
            }
        }
        boolean meetsStrictBar = strictRule.isHealthy(overall);

        return new ScoreResult(sleepScore, waterScore, exerciseScore, stressScore, overall, tier,
                buildSummaryText(data), metricsNeedingAttention, meetsStrictBar);
    }

    private static String buildSummaryText(HealthData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mood today: ").append(data.getMood()).append(". ");

        if (data.getSleepHours() < data.getSleepGoalHours()) {
            sb.append("You're a bit short on sleep — try to wind down earlier tonight. ");
        } else {
            sb.append("Nice, you hit your sleep goal. ");
        }

        if (data.getWaterGlasses() < data.getWaterGoalGlasses()) {
            sb.append("Drink a few more glasses of water today. ");
        } else {
            sb.append("Hydration looks solid. ");
        }

        if (data.getExerciseMinutes() < data.getExerciseGoalMinutes()) {
            sb.append("A short walk could help you reach your activity goal. ");
        } else {
            sb.append("Great job staying active. ");
        }

        if (data.getStressLevel() > data.getStressComfortMax()) {
            sb.append("Stress is running a little high — consider a short break or some deep breathing.");
        } else {
            sb.append("Stress levels look manageable.");
        }

        return sb.toString();
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
