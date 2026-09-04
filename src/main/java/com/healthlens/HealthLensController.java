package com.healthlens;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;
import java.util.Locale;

/**
 * Controller for HealthLens.fxml.
 * Field names below are bound to fx:id values in the FXML via @FXML.
 * If you rename an fx:id in Scene Builder, rename the matching field here too.
 */
public class HealthLensController {

    // --- Background layer ---
    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImageView;
    @FXML private StackPane scrimPane;

    // --- Inputs ---
    @FXML private Slider sleepSlider;
    @FXML private Label sleepValueLabel;

    @FXML private Slider waterSlider;
    @FXML private Label waterValueLabel;

    @FXML private Slider exerciseSlider;
    @FXML private Label exerciseValueLabel;

    @FXML private Slider stressSlider;
    @FXML private Label stressValueLabel;

    @FXML private ChoiceBox<String> moodChoiceBox;

    // --- Outputs: progress bars ---
    @FXML private ProgressBar sleepBar;
    @FXML private ProgressBar waterBar;
    @FXML private ProgressBar exerciseBar;
    @FXML private ProgressBar stressBar;

    // --- Outputs: chart ---
    @FXML private BarChart<String, Number> healthChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // --- Outputs: summary ---
    @FXML private Label overallScoreLabel;
    @FXML private Label summaryLabel;

    // Goals used for scoring
    private static final double SLEEP_GOAL_HOURS = 8.0;
    private static final double WATER_GOAL_GLASSES = 8.0;
    private static final double EXERCISE_GOAL_MINUTES = 30.0;
    private static final double STRESS_COMFORT_MAX = 4.0; // stress <= 4 is considered "in control"

    private XYChart.Series<String, Number> chartSeries;

    @FXML
    public void initialize() {
        setupBackgroundImage();
        setupMoodChoices();
        setupLiveValueLabels();
        setupChart();

        // Show an initial snapshot on load
        handleUpdate();
    }

    /**
     * Loads /com/healthlens/background.jpg from resources and stretches it to
     * fill the window. If the file is missing, falls back to a plain gradient
     * (set in styles.css on #rootPane) so the app never crashes because an
     * image wasn't supplied yet.
     *
     * TO CHANGE THE BACKGROUND IMAGE:
     * Drop your image file into src/main/resources/com/healthlens/ and rename
     * it to "background.jpg" (or edit the path below).
     */
    private void setupBackgroundImage() {
        try (InputStream is = getClass().getResourceAsStream("background.jpg")) {
            if (is != null) {
                Image image = new Image(is);
                backgroundImageView.setImage(image);
            } else {
                System.out.println("[HealthLens] No background.jpg found in resources — "
                        + "using fallback background color. See styles.css (#root-pane).");
            }
        } catch (Exception e) {
            System.out.println("[HealthLens] Could not load background image: " + e.getMessage());
        }

        // Keep the image filling the entire window at all times, including on resize.
        backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());

        // Keep the readability scrim the same size as the window too.
        scrimPane.prefWidthProperty().bind(rootPane.widthProperty());
        scrimPane.prefHeightProperty().bind(rootPane.heightProperty());
    }

    private void setupMoodChoices() {
        moodChoiceBox.setItems(FXCollections.observableArrayList(
                "Great", "Good", "Okay", "Low", "Stressed"
        ));
        moodChoiceBox.setValue("Okay");
    }

    private void setupLiveValueLabels() {
        sleepSlider.valueProperty().addListener((obs, oldV, newV) ->
                sleepValueLabel.setText(String.format(Locale.US, "%.1f h", newV.doubleValue())));

        waterSlider.valueProperty().addListener((obs, oldV, newV) ->
                waterValueLabel.setText(String.format(Locale.US, "%d", Math.round(newV.doubleValue()))));

        exerciseSlider.valueProperty().addListener((obs, oldV, newV) ->
                exerciseValueLabel.setText(String.format(Locale.US, "%d min", Math.round(newV.doubleValue()))));

        stressSlider.valueProperty().addListener((obs, oldV, newV) ->
                stressValueLabel.setText(String.format(Locale.US, "%d", Math.round(newV.doubleValue()))));
    }

    private void setupChart() {
        xAxis.setCategories(FXCollections.observableArrayList("Sleep", "Water", "Exercise", "Stress ctrl"));
        chartSeries = new XYChart.Series<>();
        chartSeries.getData().add(new XYChart.Data<>("Sleep", 0));
        chartSeries.getData().add(new XYChart.Data<>("Water", 0));
        chartSeries.getData().add(new XYChart.Data<>("Exercise", 0));
        chartSeries.getData().add(new XYChart.Data<>("Stress ctrl", 0));
        healthChart.getData().add(chartSeries);
    }

    /**
     * Reads all inputs, recomputes progress bars / chart / summary text.
     * Wired to the "Update Dashboard" button via onAction="#handleUpdate".
     */
    @FXML
    private void handleUpdate() {
        double sleepHours = sleepSlider.getValue();
        double waterGlasses = waterSlider.getValue();
        double exerciseMinutes = exerciseSlider.getValue();
        double stressLevel = stressSlider.getValue();
        String mood = moodChoiceBox.getValue();

        double sleepScore = clamp01(sleepHours / SLEEP_GOAL_HOURS);
        double waterScore = clamp01(waterGlasses / WATER_GOAL_GLASSES);
        double exerciseScore = clamp01(exerciseMinutes / EXERCISE_GOAL_MINUTES);
        // Stress is inverted: 10 (max stress) -> 0 score, 1 (min stress) -> ~1 score
        double stressScore = clamp01(1.0 - ((stressLevel - 1.0) / 9.0));

        updateBar(sleepBar, sleepScore);
        updateBar(waterBar, waterScore);
        updateBar(exerciseBar, exerciseScore);
        updateBar(stressBar, stressScore);

        chartSeries.getData().get(0).setYValue(sleepScore * 100);
        chartSeries.getData().get(1).setYValue(waterScore * 100);
        chartSeries.getData().get(2).setYValue(exerciseScore * 100);
        chartSeries.getData().get(3).setYValue(stressScore * 100);

        double overall = (sleepScore + waterScore + exerciseScore + stressScore) / 4.0;
        renderSummary(overall, mood, sleepHours, waterGlasses, exerciseMinutes, stressLevel);
    }

    private void updateBar(ProgressBar bar, double score) {
        bar.setProgress(score);
        bar.getStyleClass().removeAll("progress-good", "progress-medium", "progress-poor", "progress-neutral");
        if (score >= 0.8) {
            bar.getStyleClass().add("progress-good");
        } else if (score >= 0.5) {
            bar.getStyleClass().add("progress-medium");
        } else {
            bar.getStyleClass().add("progress-poor");
        }
    }

    private void renderSummary(double overall, String mood, double sleep, double water,
                                double exercise, double stress) {
        int percent = (int) Math.round(overall * 100);
        overallScoreLabel.setText("Overall Score: " + percent + "%");

        overallScoreLabel.getStyleClass().removeAll("score-good", "score-medium", "score-poor");
        summaryLabel.getStyleClass().removeAll("summary-good", "summary-medium", "summary-poor");

        String tier;
        if (overall >= 0.8) {
            tier = "score-good";
            summaryLabel.getStyleClass().add("summary-good");
        } else if (overall >= 0.5) {
            tier = "score-medium";
            summaryLabel.getStyleClass().add("summary-medium");
        } else {
            tier = "score-poor";
            summaryLabel.getStyleClass().add("summary-poor");
        }
        overallScoreLabel.getStyleClass().add(tier);

        StringBuilder sb = new StringBuilder();
        sb.append("Mood today: ").append(mood).append(". ");

        if (sleep < SLEEP_GOAL_HOURS) {
            sb.append("You're a bit short on sleep — try to wind down earlier tonight. ");
        } else {
            sb.append("Nice, you hit your sleep goal. ");
        }

        if (water < WATER_GOAL_GLASSES) {
            sb.append("Drink a few more glasses of water today. ");
        } else {
            sb.append("Hydration looks solid. ");
        }

        if (exercise < EXERCISE_GOAL_MINUTES) {
            sb.append("A short walk could help you reach your activity goal. ");
        } else {
            sb.append("Great job staying active. ");
        }

        if (stress > STRESS_COMFORT_MAX) {
            sb.append("Stress is running a little high — consider a short break or some deep breathing.");
        } else {
            sb.append("Stress levels look manageable.");
        }

        summaryLabel.setText(sb.toString());
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
