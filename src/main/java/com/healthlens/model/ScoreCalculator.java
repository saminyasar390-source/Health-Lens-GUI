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
        double sleepScore = clamp01(data.getSleepHours() / data.getSleepGoalHours());
        double waterScore = clamp01(data.getWaterGlasses() / data.getWaterGoalGlasses());
        double exerciseScore = clamp01(data.getExerciseMinutes() / data.getExerciseGoalMinutes());
        // Stress is inverted: a high stress level should produce a LOW score.
        double stressScore = clamp01(1.0 - ((data.getStressLevel() - 1.0) / 9.0));

        double overall = (sleepScore + waterScore + exerciseScore + stressScore) / 4.0;

        String tier;
        if (overall >= 0.8) {
            tier = "good";
        } else if (overall >= 0.5) {
            tier = "medium";
        } else {
            tier = "poor";
        }

        String summary = buildSummaryText(data);

        return new ScoreResult(sleepScore, waterScore, exerciseScore, stressScore, overall, tier, summary);
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
