package com.healthlens;

import com.healthlens.concurrency.ActivityCenterController;
import com.healthlens.concurrency.BackgroundServices;
import com.healthlens.concurrency.BreathingSession;
import com.healthlens.concurrency.HealthSnapshot;
import com.healthlens.concurrency.MergedReading;
import com.healthlens.concurrency.Notification;
import com.healthlens.concurrency.SourceReading;
import com.healthlens.concurrency.SyncService;
import com.healthlens.model.HealthData;
import com.healthlens.model.ScoreCalculator;
import com.healthlens.model.ScoreResult;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * CONTROLLER for HealthLens.fxml.
 *
 * This class's only job is to mediate between the VIEW (the FXML controls
 * below) and the MODEL (com.healthlens.model.HealthData / ScoreCalculator /
 * ScoreResult). It reads input controls, asks the Model to do the actual
 * health-scoring work, and pushes the Model's results back onto the UI.
 * It intentionally contains NO scoring formulas or health-summary wording —
 * that all lives in ScoreCalculator, which has no idea JavaFX exists.
 *
 * Field names below are bound to fx:id values in the FXML via @FXML.
 * If you rename an fx:id in Scene Builder, rename the matching field here too.
 */
public class HealthLensController {

    // --- Background layer ---
    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImageView;
    @FXML private StackPane scrimPane;

    // --- Top bar ---
    @FXML private Button themeToggleButton;
    @FXML private Button settingsButton;
    @FXML private Button chatButton;
    @FXML private Button activityCenterButton;
    @FXML private Label subtitleLabel;
    @FXML private Label reminderBanner;
    @FXML private StackPane profileAvatarStack;
    @FXML private Circle profileCircleBg;
    @FXML private Label profileInitialsLabel;
    @FXML private ImageView profileImageView;

    // --- Inputs ---
    @FXML private Slider sleepSlider;
    @FXML private Label sleepValueLabel;

    @FXML private Slider waterSlider;
    @FXML private Label waterValueLabel;

    @FXML private Slider exerciseSlider;
    @FXML private Label exerciseValueLabel;

    @FXML private Slider stressSlider;
    @FXML private Label stressValueLabel;
    @FXML private Button breathingButton;
    @FXML private Label breathingStatusLabel;

    @FXML private ChoiceBox<String> moodChoiceBox;

    @FXML private Button updateButton;
    @FXML private Button resetButton;
    @FXML private Button syncButton;

    // --- Outputs: progress bars + goal labels ---
    @FXML private ProgressBar sleepBar;
    @FXML private ProgressBar waterBar;
    @FXML private ProgressBar exerciseBar;
    @FXML private ProgressBar stressBar;

    @FXML private Label sleepGoalLabel;
    @FXML private Label waterGoalLabel;
    @FXML private Label exerciseGoalLabel;
    @FXML private Label stressGoalLabel;

    // --- Outputs: chart ---
    @FXML private BarChart<String, Number> healthChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // --- Outputs: summary ---
    @FXML private Label overallScoreLabel;
    @FXML private Label summaryLabel;
    @FXML private HBox batterySegments;
    @FXML private Label batteryPercentLabel;

    private final List<Region> batterySegmentNodes = new ArrayList<>();

    /** THE MODEL. Everything about "what the data means" is delegated to this + ScoreCalculator. */
    private final HealthData healthData = new HealthData();

    private boolean darkMode = false;
    private XYChart.Series<String, Number> chartSeries;

    // Where saved settings/preferences live (per OS user, no file management needed)
    private final Preferences prefs = Preferences.userNodeForPackage(HealthLensController.class);

    @FXML
    public void initialize() {
        loadPreferences();
        setupBackgroundImage();
        setupMoodChoices();
        setupLiveValueLabels();
        setupChart();
        setupKeyboardShortcut();
        updateGoalLabels();
        applyThemeWhenSceneReady();
        setupProfileAvatar();
        setupBatteryIndicator();
        Tooltip.install(profileAvatarStack, new Tooltip("Your profile — click to change picture, email, or background"));
        startBackgroundServices();

        // Show an initial snapshot on load
        handleUpdate();
    }

    /** Set once a user logs in/signs up; used to key per-user profile preferences. */
    private String currentUsername;
    private String currentEmail;

    /**
     * Called by GuideController right after loading this screen, if the
     * user came in through Login/Signup -> Guide -> HealthLens. Demonstrates
     * passing data between scenes: the username and email typed earlier end
     * up here, and are used to load this user's saved profile picture and
     * background image (if any were set previously).
     */
    public void initSession(String userName, String userEmail) {
        this.currentUsername = userName;
        this.currentEmail = userEmail;
        if (userName != null && !userName.isBlank()) {
            subtitleLabel.setText("Welcome back, " + userName + "! Your daily health habits, visualized.");
        }
        setupProfileAvatar();
        applyCustomBackgroundIfAny();
    }

    // ===================== BACKGROUND IMAGE =====================

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

    // ===================== SETUP =====================

    private void setupMoodChoices() {
        moodChoiceBox.setItems(FXCollections.observableArrayList(
                "Great", "Good", "Okay", "Low", "Stressed"
        ));
        moodChoiceBox.setValue(healthData.getMood());
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

    /** Enter key anywhere in the window triggers "Update Dashboard". */
    private void setupKeyboardShortcut() {
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        handleUpdate();
                    }
                });
            }
        });
    }

    // ===================== BACKGROUND SERVICES =====================

    /**
     * Every thread, pool and queue in HealthLens lives behind this one
     * object. The dashboard starts it once and shuts it down once. Nothing
     * else in the UI layer ever creates a Thread directly, which is the
     * whole reason the app can be closed cleanly instead of leaving worker
     * threads running and the JVM refusing to exit.
     */
    private final BackgroundServices services = new BackgroundServices();

    private void startBackgroundServices() {
        services.start();

        // Reminders are produced on one background thread and delivered on
        // another. This callback arrives on the notifier thread, so the
        // banner update has to hop back onto the JavaFX thread first.
        services.getNotificationCenter().addListener(notification ->
                Platform.runLater(() -> showReminderBanner(notification)));

        // One place to stop everything when the window closes.
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o2, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.setOnCloseRequest(e -> services.shutdown());
                    }
                });
            }
        });
    }

    private void showReminderBanner(Notification notification) {
        reminderBanner.setText(notification.getText());
        reminderBanner.setVisible(true);
        reminderBanner.setManaged(true);
        updateNotificationBadge();

        PauseTransition hide = new PauseTransition(Duration.seconds(8));
        hide.setOnFinished(e -> {
            reminderBanner.setVisible(false);
            reminderBanner.setManaged(false);
        });
        hide.play();
    }

    /** Shows the unread count on the bell, the way any notification UI does. */
    private void updateNotificationBadge() {
        int unread = services.getNotificationCenter().getUnreadCount();
        activityCenterButton.setText(unread == 0 ? "▥" : "▥ " + unread);
    }

    // ===================== GUIDED BREATHING =====================

    /**
     * Starts or stops the breathing exercise. The session itself lives in
     * BreathingSession; this method's only job is to translate its
     * callbacks back onto the JavaFX thread and keep the button label in
     * step with reality.
     */
    @FXML
    private void handleToggleBreathing() {
        BreathingSession session = services.getBreathingSession();

        if (session.isRunning()) {
            session.stop();   // cooperative: asks, never forces
            return;
        }

        breathingButton.setText("\u23F9 Stop");

        session.start(3, new BreathingSession.Listener() {
            @Override
            public void onPhase(String phase, int phaseIndex, int totalPhases) {
                Platform.runLater(() ->
                        breathingStatusLabel.setText(phase + "  (" + phaseIndex + "/" + totalPhases + ")"));
            }

            @Override
            public void onFinished(boolean completedFully) {
                Platform.runLater(() -> {
                    breathingButton.setText("\uD83E\uDDD8 Guided Breathing");
                    if (completedFully) {
                        breathingStatusLabel.setText("Done — nicely paced.");
                        // A finished session earns a real reward: stress
                        // drops a point on the actual dashboard.
                        stressSlider.setValue(Math.max(stressSlider.getMin(), stressSlider.getValue() - 1));
                        handleUpdate();
                    } else {
                        breathingStatusLabel.setText("Stopped early — that's okay too.");
                    }
                });
            }
        });
    }

    // ===================== SYNC ALL SOURCES =====================

    /**
     * Fetches every connected source at the same time. All of the executor
     * work is inside SyncService; what stays here is the UI half — disable
     * the button, show progress as each source reports, apply the merged
     * result when everything is in.
     *
     * Note what is NOT here: no future.get(), no join(), no sleep(). A
     * button handler runs on the JavaFX thread, and blocking it for even a
     * second would visibly freeze the window.
     */
    @FXML
    private void handleSyncWithDevice() {
        SyncService sync = services.getSyncService();

        boolean started = sync.syncAll(new SyncService.Listener() {
            @Override
            public void onSyncStarted(java.util.List<String> sourceNames) {
                Platform.runLater(() -> {
                    syncButton.setDisable(true);
                    syncButton.setText("Syncing 0/" + sourceNames.size() + "…");
                });
            }

            @Override
            public void onSourceFinished(SourceReading reading) {
                Platform.runLater(() -> {
                    int done = ++sourcesDone;
                    syncButton.setText("Syncing " + done + "/" + sync.getSourceNames().size() + "…");
                });
            }

            @Override
            public void onSyncFinished(MergedReading merged, java.util.List<SourceReading> readings) {
                Platform.runLater(() -> {
                    applyMergedReading(merged);
                    sourcesDone = 0;
                    syncButton.setDisable(false);
                    syncButton.setText("\uD83D\uDD04 Sync All Sources");
                });
            }
        });

        if (!started) {
            syncButton.setText("Already syncing…");
        }
    }

    /** Counter for the progress label. Only ever touched on the FX thread. */
    private int sourcesDone = 0;

    /** Applies whatever came back, leaving anything that failed untouched. */
    private void applyMergedReading(MergedReading merged) {
        if (merged.getSleepHours() != null) {
            sleepSlider.setValue(clamp(merged.getSleepHours(), sleepSlider.getMin(), sleepSlider.getMax()));
        }
        if (merged.getWaterGlasses() != null) {
            waterSlider.setValue(clamp(merged.getWaterGlasses(), waterSlider.getMin(), waterSlider.getMax()));
        }
        if (merged.getExerciseMinutes() != null) {
            exerciseSlider.setValue(clamp(merged.getExerciseMinutes(), exerciseSlider.getMin(), exerciseSlider.getMax()));
        }
        if (merged.getStressLevel() != null) {
            stressSlider.setValue(clamp(merged.getStressLevel(), stressSlider.getMin(), stressSlider.getMax()));
        }
        handleUpdate();
    }

    // ===================== ACTIVITY CENTRE =====================

    @FXML
    private void handleOpenActivityCenter() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/healthlens/concurrency/ActivityCenter.fxml"));
            Parent centreRoot = loader.load();

            ActivityCenterController centreController = loader.getController();
            centreController.init(services);

            Scene centreScene = new Scene(centreRoot, 560, 620);
            centreScene.getStylesheets().add(
                    getClass().getResource(darkMode ? "styles-dark.css" : "styles.css").toExternalForm());

            Stage centreStage = new Stage();
            centreStage.setTitle("HealthLens — Activity Centre");
            centreStage.setScene(centreScene);
            centreStage.setOnCloseRequest(e -> centreController.dispose());
            centreStage.show();

            // Opening the centre counts as reading your reminders.
            services.getNotificationCenter().markAllRead();
            updateNotificationBadge();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateGoalLabels() {
        sleepGoalLabel.setText(String.format(Locale.US, "Sleep goal (%.0fh)", healthData.getSleepGoalHours()));
        waterGoalLabel.setText(String.format(Locale.US, "Water goal (%.0f glasses)", healthData.getWaterGoalGlasses()));
        exerciseGoalLabel.setText(String.format(Locale.US, "Exercise goal (%.0f min)", healthData.getExerciseGoalMinutes()));
        stressGoalLabel.setText(String.format(Locale.US, "Stress control (comfortable ≤ %.0f)", healthData.getStressComfortMax()));
    }

    // ===================== THEME (LIGHT / DARK) =====================

    private void applyThemeWhenSceneReady() {
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyTheme(newScene);
            }
        });
    }

    @FXML
    private void handleToggleTheme() {
        darkMode = !darkMode;
        Scene scene = rootPane.getScene();
        if (scene != null) {
            applyTheme(scene);
        }
        prefs.putBoolean("darkMode", darkMode);
    }

    private void applyTheme(Scene scene) {
        String stylesheet = darkMode ? "styles-dark.css" : "styles.css";
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
        themeToggleButton.setText(darkMode ? "☀" : "◐");
    }

    // ===================== PROFILE (AVATAR / EMAIL / BACKGROUND) =====================

    private void setupProfileAvatar() {
        Circle clip = new Circle(17, 17, 17);
        profileImageView.setClip(clip);
        profileCircleBg.setFill(Color.web("#0ea5a1"));

        String initial = (currentUsername != null && !currentUsername.isBlank())
                ? currentUsername.substring(0, 1).toUpperCase() : "?";
        profileInitialsLabel.setText(initial);

        String savedPicturePath = currentUsername == null ? null
                : prefs.get("profile." + currentUsername + ".picturePath", null);
        if (savedPicturePath != null && new File(savedPicturePath).exists()) {
            profileImageView.setImage(new Image(new File(savedPicturePath).toURI().toString()));
            profileImageView.setVisible(true);
            profileInitialsLabel.setVisible(false);
        } else {
            profileImageView.setVisible(false);
            profileInitialsLabel.setVisible(true);
        }
    }

    private void applyCustomBackgroundIfAny() {
        if (currentUsername == null) {
            return;
        }
        String savedBackgroundPath = prefs.get("profile." + currentUsername + ".backgroundPath", null);
        if (savedBackgroundPath != null && new File(savedBackgroundPath).exists()) {
            backgroundImageView.setImage(new Image(new File(savedBackgroundPath).toURI().toString()));
        }
    }

    @FXML
    private void handleOpenProfile() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Your Profile");
        dialog.setHeaderText(currentUsername == null ? "Profile" : "Signed in as " + currentUsername);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource(darkMode ? "styles-dark.css" : "styles.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(16));

        // --- profile picture ---
        ImageView previewView = new ImageView();
        previewView.setFitWidth(60);
        previewView.setFitHeight(60);
        previewView.setClip(new Circle(30, 30, 30));
        if (profileImageView.getImage() != null) {
            previewView.setImage(profileImageView.getImage());
        }
        Button changePictureButton = new Button("Change Picture...");
        changePictureButton.getStyleClass().add("reset-button");
        changePictureButton.setOnAction(e -> {
            File file = chooseImageFile("Choose a profile picture");
            if (file != null && currentUsername != null) {
                prefs.put("profile." + currentUsername + ".picturePath", file.getAbsolutePath());
                previewView.setImage(new Image(file.toURI().toString()));
                setupProfileAvatar();
            }
        });
        grid.add(new Label("Profile picture:"), 0, 0);
        grid.add(new HBox(10, previewView, changePictureButton), 1, 0);

        // --- email ---
        TextField emailField = new TextField(currentEmail == null ? "" : currentEmail);
        emailField.setPrefWidth(200);
        Button saveEmailButton = new Button("Save");
        saveEmailButton.getStyleClass().add("reset-button");
        saveEmailButton.setOnAction(e -> {
            if (currentUsername != null) {
                currentEmail = emailField.getText();
                prefs.put("user." + currentUsername + ".email", currentEmail);
                Alert confirm = new Alert(Alert.AlertType.INFORMATION);
                confirm.setTitle("Email updated");
                confirm.setHeaderText(null);
                confirm.setContentText("Your email has been updated.");
                confirm.showAndWait();
            }
        });
        grid.add(new Label("Email:"), 0, 1);
        grid.add(new HBox(10, emailField, saveEmailButton), 1, 1);

        // --- background image ---
        Button changeBackgroundButton = new Button("Change Background Image...");
        changeBackgroundButton.getStyleClass().add("reset-button");
        changeBackgroundButton.setOnAction(e -> {
            File file = chooseImageFile("Choose a background image");
            if (file != null) {
                backgroundImageView.setImage(new Image(file.toURI().toString()));
                if (currentUsername != null) {
                    prefs.put("profile." + currentUsername + ".backgroundPath", file.getAbsolutePath());
                }
            }
        });
        grid.add(new Label("Background image:"), 0, 2);
        grid.add(changeBackgroundButton, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private File chooseImageFile(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        Stage stage = (Stage) profileAvatarStack.getScene().getWindow();
        return fileChooser.showOpenDialog(stage);
    }

    // ===================== SETTINGS DIALOG (CUSTOM GOALS) =====================

    @FXML
    private void handleOpenSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Customize Your Goals");
        dialog.setHeaderText("Set the daily targets HealthLens should track you against.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        Spinner<Double> sleepGoalSpinner = new Spinner<>(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 12, healthData.getSleepGoalHours(), 0.5));
        Spinner<Double> waterGoalSpinner = new Spinner<>(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 15, healthData.getWaterGoalGlasses(), 1));
        Spinner<Double> exerciseGoalSpinner = new Spinner<>(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(5, 120, healthData.getExerciseGoalMinutes(), 5));
        Spinner<Double> stressMaxSpinner = new Spinner<>(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 10, healthData.getStressComfortMax(), 1));

        for (Spinner<Double> s : new Spinner[]{sleepGoalSpinner, waterGoalSpinner, exerciseGoalSpinner, stressMaxSpinner}) {
            s.setEditable(true);
            s.setPrefWidth(100);
        }

        grid.add(new Label("Sleep goal (hours):"), 0, 0);
        grid.add(sleepGoalSpinner, 1, 0);
        grid.add(new Label("Water goal (glasses):"), 0, 1);
        grid.add(waterGoalSpinner, 1, 1);
        grid.add(new Label("Exercise goal (minutes):"), 0, 2);
        grid.add(exerciseGoalSpinner, 1, 2);
        grid.add(new Label("Stress comfortable at or below:"), 0, 3);
        grid.add(stressMaxSpinner, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Match the app's current theme so the popup doesn't feel out of place
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource(darkMode ? "styles-dark.css" : "styles.css").toExternalForm());

        dialog.showAndWait().ifPresent(result -> {
            if (result == saveButtonType) {
                healthData.setSleepGoalHours(sleepGoalSpinner.getValue());
                healthData.setWaterGoalGlasses(waterGoalSpinner.getValue());
                healthData.setExerciseGoalMinutes(exerciseGoalSpinner.getValue());
                healthData.setStressComfortMax(stressMaxSpinner.getValue());

                savePreferences();
                updateGoalLabels();
                handleUpdate();
            }
        });
    }

    // ===================== CHAT ASSISTANT =====================

    /**
     * Opens a small chat window where the user can type sentences like
     * "I slept 7 hours and drank 4 glasses of water, feeling stressed".
     * ChatParser (rule-based, no ML model) extracts whatever it can. The
     * Controller applies matched values to the sliders/mood (the VIEW),
     * then calls handleUpdate(), which is the normal path that copies View
     * values into the Model and re-scores.
     */
    @FXML
    private void handleOpenChat() {
        Stage chatStage = new Stage();
        chatStage.setTitle("HealthLens Assistant");
        chatStage.initModality(Modality.NONE);
        chatStage.initOwner(rootPane.getScene() != null ? rootPane.getScene().getWindow() : null);

        ListView<String> chatLog = new ListView<>();
        chatLog.getStyleClass().add("chat-log");
        chatLog.setPrefHeight(320);
        chatLog.setFocusTraversable(false);
        chatLog.getItems().add("Bot: Tell me about your day — e.g. \"I slept 7 hours, drank "
                + "5 glasses of water, exercised 20 minutes, feeling stressed.\"");

        TextField input = new TextField();
        input.setPromptText("Type a message and press Enter...");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.getStyleClass().add("primary-button");

        Runnable sendMessage = () -> {
            String text = input.getText();
            if (text == null || text.isBlank()) {
                return;
            }
            chatLog.getItems().add("You: " + text);
            String reply = processChatMessage(text);
            chatLog.getItems().add("Bot: " + reply);
            chatLog.scrollTo(chatLog.getItems().size() - 1);
            input.clear();
        };

        sendButton.setOnAction(e -> sendMessage.run());
        input.setOnAction(e -> sendMessage.run());

        HBox inputRow = new HBox(8, input, sendButton);
        VBox root = new VBox(10, chatLog, inputRow);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("chat-window");

        Scene chatScene = new Scene(root, 420, 440);
        chatScene.getStylesheets().add(
                getClass().getResource(darkMode ? "styles-dark.css" : "styles.css").toExternalForm());

        chatStage.setScene(chatScene);
        chatStage.show();
    }

    /**
     * Runs the typed message through ChatParser, applies any values it
     * found to the real sliders/mood picker, refreshes the dashboard, and
     * returns a human-readable reply describing what was understood.
     */
    private String processChatMessage(String message) {
        ChatParser.ChatUpdate update = ChatParser.parse(message);

        if (update.isEmpty()) {
            return "I couldn't pick anything out of that — try mentioning hours of sleep, "
                    + "glasses of water, minutes of exercise, a stress level, or how you're feeling.";
        }

        List<String> understood = new ArrayList<>();

        if (update.sleepHours != null) {
            sleepSlider.setValue(clamp(update.sleepHours, sleepSlider.getMin(), sleepSlider.getMax()));
            understood.add(String.format(Locale.US, "sleep: %.1fh", update.sleepHours));
        }
        if (update.waterGlasses != null) {
            waterSlider.setValue(clamp(update.waterGlasses, waterSlider.getMin(), waterSlider.getMax()));
            understood.add(String.format(Locale.US, "water: %.0f glasses", update.waterGlasses));
        }
        if (update.exerciseMinutes != null) {
            exerciseSlider.setValue(clamp(update.exerciseMinutes, exerciseSlider.getMin(), exerciseSlider.getMax()));
            understood.add(String.format(Locale.US, "exercise: %.0f min", update.exerciseMinutes));
        }
        if (update.stressLevel != null) {
            stressSlider.setValue(clamp(update.stressLevel, stressSlider.getMin(), stressSlider.getMax()));
            understood.add(String.format(Locale.US, "stress: %.0f/10", update.stressLevel));
        }
        if (update.mood != null) {
            moodChoiceBox.setValue(update.mood);
            understood.add("mood: " + update.mood);
        }

        handleUpdate();

        return "Got it — updated " + String.join(", ", understood) + ". Dashboard refreshed!";
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ===================== UPDATE (VIEW -> MODEL -> VIEW) =====================

    /**
     * The core Controller flow:
     *   1) Read the VIEW (sliders/mood) into the MODEL (healthData)
     *   2) Ask the MODEL (via ScoreCalculator) to compute a ScoreResult
     *   3) Push that ScoreResult back onto the VIEW (bars, chart, summary)
     * No scoring math or health advice text lives in this method — see
     * ScoreCalculator for that.
     */
    @FXML
    private void handleUpdate() {
        // 1) View -> Model
        healthData.setSleepHours(sleepSlider.getValue());
        healthData.setWaterGlasses(waterSlider.getValue());
        healthData.setExerciseMinutes(exerciseSlider.getValue());
        healthData.setStressLevel(stressSlider.getValue());
        healthData.setMood(moodChoiceBox.getValue());

        // 2) Model does the work
        ScoreResult result = ScoreCalculator.calculate(healthData);

        // 3) Model -> View
        animateBar(sleepBar, result.getSleepScore());
        animateBar(waterBar, result.getWaterScore());
        animateBar(exerciseBar, result.getExerciseScore());
        animateBar(stressBar, result.getStressScore());

        chartSeries.getData().get(0).setYValue(result.getSleepScore() * 100);
        chartSeries.getData().get(1).setYValue(result.getWaterScore() * 100);
        chartSeries.getData().get(2).setYValue(result.getExerciseScore() * 100);
        chartSeries.getData().get(3).setYValue(result.getStressScore() * 100);

        renderSummary(result);
        savePreferences();

        // 4) Hand the background reminder thread an immutable copy of where
        //    the user now stands. It must never read the sliders itself.
        services.getReminderService().publishSnapshot(new HealthSnapshot(
                healthData.getSleepHours(),
                healthData.getWaterGlasses(),
                healthData.getExerciseMinutes(),
                healthData.getStressLevel(),
                healthData.getSleepGoalHours(),
                healthData.getWaterGoalGlasses(),
                healthData.getExerciseGoalMinutes(),
                healthData.getStressComfortMax()));
    }

    /** Opens a separate recommendations window based on the current dashboard inputs. */
    @FXML
    private void handleOpenRecommendations() {
        VBox content = new VBox(12);
        content.getStyleClass().add("recommendations-content");
        content.setPadding(new Insets(18));

        Label heading = new Label("Your Personalized Health Recommendations");
        heading.getStyleClass().add("recommendations-heading");
        Label intro = new Label("Suggestions are based on your current entries and are not medical advice.");
        intro.getStyleClass().add("recommendations-intro");
        content.getChildren().addAll(heading, intro);

        addRecommendation(content, "💤 Sleep", sleepSlider.getValue() < 7,
                "Try to build a consistent sleep routine and aim for around 7–9 hours.",
                "Your sleep duration is within a reasonable daily range. Keep your sleep schedule consistent.");
        addRecommendation(content, "💧 Hydration", waterSlider.getValue() < healthData.getWaterGoalGlasses(),
                "Your water intake is below your configured goal. Drink water regularly throughout the day.",
                "You are meeting your configured water goal. Continue spreading your intake across the day.");
        addRecommendation(content, "🏃 Exercise", exerciseSlider.getValue() < healthData.getExerciseGoalMinutes(),
                "Your exercise is below your daily goal. Consider a manageable walk or light activity.",
                "You are meeting your exercise goal. Maintain a routine that feels sustainable.");
        addRecommendation(content, "🧠 Stress", stressSlider.getValue() >= 7,
                "Your stress level is high. Try guided breathing, a short break, or talking to someone you trust.",
                "Your reported stress level is not in the high range. Keep using healthy coping habits.");
        addRecommendation(content, "🙂 Mood", "Low".equalsIgnoreCase(moodChoiceBox.getValue()) || "Stressed".equalsIgnoreCase(moodChoiceBox.getValue()),
                "Your selected mood suggests you may benefit from rest, support, or a calming activity.",
                "Your selected mood is positive or neutral. Continue activities that support your wellbeing.");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Personalized Health Recommendations");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("recommendations-dialog");
        dialog.getDialogPane().setPrefWidth(560);
        dialog.showAndWait();
    }

    private void addRecommendation(VBox content, String title, boolean needsAttention,
                                   String attentionText, String positiveText) {
        VBox card = new VBox(5);
        card.getStyleClass().add("recommendation-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("recommendation-title");
        Label message = new Label(needsAttention ? attentionText : positiveText);
        message.setWrapText(true);
        message.getStyleClass().add(needsAttention ? "recommendation-attention" : "recommendation-positive");
        card.getChildren().addAll(titleLabel, message);
        content.getChildren().add(card);
    }

    @FXML
    private void handleReset() {
        healthData.resetEntriesToDefaults();
        sleepSlider.setValue(healthData.getSleepHours());
        waterSlider.setValue(healthData.getWaterGlasses());
        exerciseSlider.setValue(healthData.getExerciseMinutes());
        stressSlider.setValue(healthData.getStressLevel());
        moodChoiceBox.setValue(healthData.getMood());
        handleUpdate();
    }

    /** Smoothly animates a progress bar to its new value instead of jumping instantly. */
    private void animateBar(ProgressBar bar, double target) {
        bar.getStyleClass().removeAll("progress-good", "progress-medium", "progress-poor", "progress-neutral");
        if (target >= 0.8) {
            bar.getStyleClass().add("progress-good");
        } else if (target >= 0.5) {
            bar.getStyleClass().add("progress-medium");
        } else {
            bar.getStyleClass().add("progress-poor");
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(450),
                        new KeyValue(bar.progressProperty(), target, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    /** Creates the five visual segments used by the mobile-style battery indicator. */
    private void setupBatteryIndicator() {
        batterySegments.getChildren().clear();
        batterySegmentNodes.clear();

        for (int i = 0; i < 10; i++) {
            Region segment = new Region();
            segment.getStyleClass().add("battery-segment");
            segment.setPrefSize(9, 30);
            segment.setMinSize(9, 30);
            segment.setMaxSize(9, 30);
            batterySegmentNodes.add(segment);
        }

        // Add segments from left to right like a mobile battery indicator.
        batterySegments.getChildren().addAll(batterySegmentNodes);
        batteryPercentLabel.setTooltip(new Tooltip("Battery percentage is calculated from your overall health score."));
    }

    /** Updates the battery using the existing overall health score. */
    private void updateBatteryIndicator(ScoreResult result) {
        int percent = result.getOverallPercent();
        int filledSegments = (int) Math.ceil(percent / 10.0);
        String batteryTier = "battery-" + result.getTier();

        batteryPercentLabel.setText(percent + "%");
        ScaleTransition pulse = new ScaleTransition(Duration.millis(220), batteryPercentLabel);
        pulse.setFromX(0.92);
        pulse.setFromY(0.92);
        pulse.setToX(1.0);
        pulse.setToY(1.0);
        pulse.play();
        batteryPercentLabel.getStyleClass().removeAll("battery-good", "battery-medium", "battery-poor");
        batteryPercentLabel.getStyleClass().add(batteryTier);

        for (int i = 0; i < batterySegmentNodes.size(); i++) {
            Region segment = batterySegmentNodes.get(i);
            segment.getStyleClass().removeAll("battery-filled", "battery-empty", "battery-good", "battery-medium", "battery-poor");
            boolean shouldFill = i < filledSegments;
            if (shouldFill) {
                segment.getStyleClass().addAll("battery-filled", batteryTier);
            } else {
                segment.getStyleClass().add("battery-empty");
            }

            // Smoothly animate each segment whenever the score changes.
            segment.setOpacity(0.35);
            Timeline fillAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(segment.opacityProperty(), 0.35)),
                    new KeyFrame(Duration.millis(180 + (i * 70)),
                            new KeyValue(segment.opacityProperty(), shouldFill ? 1.0 : 0.55, Interpolator.EASE_BOTH))
            );
            fillAnimation.play();
        }
    }

    /** Pure UI work: takes a ScoreResult from the Model and displays it. No scoring logic here. */
    private void renderSummary(ScoreResult result) {
        updateBatteryIndicator(result);
        overallScoreLabel.setText("Overall Score: " + result.getOverallPercent() + "%");

        overallScoreLabel.getStyleClass().removeAll("score-good", "score-medium", "score-poor");
        summaryLabel.getStyleClass().removeAll("summary-good", "summary-medium", "summary-poor");

        overallScoreLabel.getStyleClass().add("score-" + result.getTier());
        summaryLabel.getStyleClass().add("summary-" + result.getTier());

        summaryLabel.setText(result.getSummaryText());
    }

    // ===================== PREFERENCES (SAVE / LOAD) =====================

    /**
     * Restores goals, last-entered habit values, mood, and theme choice from
     * the previous session straight into the Model. Uses java.util.prefs,
     * which stores small values per-OS-user automatically — no file to manage.
     */
    private void loadPreferences() {
        healthData.setSleepGoalHours(prefs.getDouble("goal.sleep", HealthData.DEFAULT_SLEEP_GOAL_HOURS));
        healthData.setWaterGoalGlasses(prefs.getDouble("goal.water", HealthData.DEFAULT_WATER_GOAL_GLASSES));
        healthData.setExerciseGoalMinutes(prefs.getDouble("goal.exercise", HealthData.DEFAULT_EXERCISE_GOAL_MINUTES));
        healthData.setStressComfortMax(prefs.getDouble("goal.stressMax", HealthData.DEFAULT_STRESS_COMFORT_MAX));
        darkMode = prefs.getBoolean("darkMode", false);

        healthData.setSleepHours(prefs.getDouble("last.sleep", HealthData.DEFAULT_SLEEP_HOURS));
        healthData.setWaterGlasses(prefs.getDouble("last.water", HealthData.DEFAULT_WATER_GLASSES));
        healthData.setExerciseMinutes(prefs.getDouble("last.exercise", HealthData.DEFAULT_EXERCISE_MINUTES));
        healthData.setStressLevel(prefs.getDouble("last.stress", HealthData.DEFAULT_STRESS_LEVEL));
        healthData.setMood(prefs.get("last.mood", HealthData.DEFAULT_MOOD));

        sleepSlider.setValue(healthData.getSleepHours());
        waterSlider.setValue(healthData.getWaterGlasses());
        exerciseSlider.setValue(healthData.getExerciseMinutes());
        stressSlider.setValue(healthData.getStressLevel());

        // Live value labels won't have fired their listeners yet at this point
        // (they're attached in setupLiveValueLabels(), called right after this),
        // so set the initial label text manually here.
        sleepValueLabel.setText(String.format(Locale.US, "%.1f h", sleepSlider.getValue()));
        waterValueLabel.setText(String.format(Locale.US, "%d", Math.round(waterSlider.getValue())));
        exerciseValueLabel.setText(String.format(Locale.US, "%d min", Math.round(exerciseSlider.getValue())));
        stressValueLabel.setText(String.format(Locale.US, "%d", Math.round(stressSlider.getValue())));
    }

    private void savePreferences() {
        prefs.putDouble("goal.sleep", healthData.getSleepGoalHours());
        prefs.putDouble("goal.water", healthData.getWaterGoalGlasses());
        prefs.putDouble("goal.exercise", healthData.getExerciseGoalMinutes());
        prefs.putDouble("goal.stressMax", healthData.getStressComfortMax());

        prefs.putDouble("last.sleep", healthData.getSleepHours());
        prefs.putDouble("last.water", healthData.getWaterGlasses());
        prefs.putDouble("last.exercise", healthData.getExerciseMinutes());
        prefs.putDouble("last.stress", healthData.getStressLevel());

        if (healthData.getMood() != null) {
            prefs.put("last.mood", healthData.getMood());
        }
    }
}
