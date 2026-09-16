package com.healthlens.model;

/**
 * MODEL: the output of scoring a HealthData snapshot. Also has zero JavaFX
 * imports — it's just numbers and strings that the Controller later maps
 * onto progress bars, a chart, and labels.
 */
public class ScoreResult {

    private final double sleepScore;
    private final double waterScore;
    private final double exerciseScore;
    private final double stressScore;
    private final double overallScore;
    private final String tier; // "good", "medium", or "poor"
    private final String summaryText;

    public ScoreResult(double sleepScore, double waterScore, double exerciseScore, double stressScore,
                        double overallScore, String tier, String summaryText) {
        this.sleepScore = sleepScore;
        this.waterScore = waterScore;
        this.exerciseScore = exerciseScore;
        this.stressScore = stressScore;
        this.overallScore = overallScore;
        this.tier = tier;
        this.summaryText = summaryText;
    }

    public double getSleepScore() { return sleepScore; }
    public double getWaterScore() { return waterScore; }
    public double getExerciseScore() { return exerciseScore; }
    public double getStressScore() { return stressScore; }
    public double getOverallScore() { return overallScore; }
    public String getTier() { return tier; }
    public String getSummaryText() { return summaryText; }

    /** Overall score as a whole-number percentage, e.g. 82 for 0.82. */
    public int getOverallPercent() {
        return (int) Math.round(overallScore * 100);
    }
}
