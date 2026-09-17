package com.healthlens.concurrency;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

/**
 * CONTROLLER for ActivityCenter.fxml.
 *
 * THE ONE RULE THIS CLASS EXISTS TO ENFORCE: JavaFX controls may only be
 * touched from the JavaFX Application Thread. Every service in this package
 * deliberately has no JavaFX imports and calls its listeners on whatever
 * background thread it happens to be using, which means every single
 * callback below has to hop back onto the FX thread with
 * Platform.runLater(...) before it touches a Label or a ListView.
 *
 * The live "Running right now" list is refreshed by a JavaFX Timeline
 * rather than by a polling thread. A Timeline already ticks on the FX
 * thread, so reading Thread.getState() and writing to Labels in the same
 * callback is safe with no locking at all - the cheapest correct answer.
 */
public class ActivityCenterController {

    @FXML private ScrollPane root;

    @FXML private CheckBox howItWorksCheck;
    @FXML private Label hintLabel;

    @FXML private VBox activityRows;

    @FXML private CheckBox remindersEnabledCheck;
    @FXML private Label inboxStatusLabel;
    @FXML private ListView<String> notificationList;

    @FXML private VBox sourceRows;
    @FXML private Button syncNowButton;
    @FXML private Button cancelSyncButton;

    @FXML private Button runDiagnosticsButton;
    @FXML private Label diagnosticsResultLabel;

    private BackgroundServices services;
    private Timeline refreshTimeline;
    private NotificationCenterService.Listener notificationListener;

    @FXML
    public void initialize() {
        notificationList.setItems(FXCollections.observableArrayList());

        howItWorksCheck.selectedProperty().addListener((obs, was, isNow) -> {
            hintLabel.setVisible(isNow);
            hintLabel.setManaged(isNow);
            refreshActivities();
            refreshSources();
        });
    }

    /** Called by the dashboard immediately after the FXML is loaded. */
    public void init(BackgroundServices services) {
        this.services = services;

        remindersEnabledCheck.setSelected(services.getReminderService().isEnabled());
        remindersEnabledCheck.selectedProperty().addListener((obs, was, isNow) ->
                services.getReminderService().setEnabled(isNow));

        // Arrives on the notification consumer thread -> hop to the FX thread.
        notificationListener = notification -> Platform.runLater(this::refreshNotifications);
        services.getNotificationCenter().addListener(notificationListener);

        services.getNotificationCenter().markAllRead();

        refreshNotifications();
        refreshSources();
        refreshActivities();

        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            refreshActivities();
            refreshInboxStatus();
            refreshSyncButtons();
        }));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    /** Called from the window's close handler so the Timeline doesn't leak. */
    public void dispose() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        if (services != null && notificationListener != null) {
            services.getNotificationCenter().removeListener(notificationListener);
        }
    }

    // ===================== RUNNING RIGHT NOW =====================

    private void refreshActivities() {
        if (services == null) {
            return;
        }
        activityRows.getChildren().clear();

        List<ActivityInfo> snapshot = services.getActivityRegistry().snapshot();
        if (snapshot.isEmpty()) {
            Label empty = new Label("Nothing running in the background right now.");
            empty.getStyleClass().add("hint-label");
            activityRows.getChildren().add(empty);
            return;
        }

        boolean detailed = howItWorksCheck.isSelected();
        for (ActivityInfo info : snapshot) {
            activityRows.getChildren().add(buildActivityRow(info, detailed));
        }
    }

    private HBox buildActivityRow(ActivityInfo info, boolean detailed) {
        Label name = new Label(info.getDisplayName());
        name.getStyleClass().add("control-label");

        Label description = new Label(detailed ? info.getLabConcept() : info.getPlainDescription());
        description.getStyleClass().add("hint-label");
        description.setWrapText(true);
        description.setMaxWidth(320.0);

        VBox left = new VBox(2.0, name, description);

        String stateText = detailed
                ? info.getThreadStateText() + "  •  " + info.getThreadName()
                : info.getFriendlyState();

        Label state = new Label(stateText);
        state.getStyleClass().add("value-label");
        state.setWrapText(true);
        state.setMaxWidth(180.0);

        Label status = new Label(info.getStatus());
        status.getStyleClass().add("hint-label");
        status.setWrapText(true);
        status.setMaxWidth(180.0);

        VBox right = new VBox(2.0, state, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10.0, left, spacer, right);
        row.getStyleClass().add("summary-box");
        return row;
    }

    // ===================== REMINDERS =====================

    private void refreshNotifications() {
        if (services == null) {
            return;
        }
        notificationList.getItems().clear();
        for (Notification notification : services.getNotificationCenter().getHistoryNewestFirst()) {
            notificationList.getItems().add(notification.getTimeText() + "   " + notification.getText());
        }
        if (notificationList.getItems().isEmpty()) {
            notificationList.getItems().add("No reminders yet — HealthLens only nudges you when you're behind on a goal.");
        }
        refreshInboxStatus();
    }

    private void refreshInboxStatus() {
        if (services == null) {
            return;
        }
        NotificationQueue<Notification> inbox = services.getInbox();
        int waiting = inbox.size();
        String text = "Inbox: " + waiting + " of " + inbox.getCapacity() + " waiting";
        if (inbox.isFull()) {
            text += "  —  full, so new reminders are being held back until you catch up";
        }
        inboxStatusLabel.setText(text);
    }

    @FXML
    private void handleMarkAllRead() {
        if (services == null) {
            return;
        }
        services.getNotificationCenter().markAllRead();
        refreshNotifications();
    }

    // ===================== DATA SOURCES =====================

    private void refreshSources() {
        if (services == null) {
            return;
        }
        sourceRows.getChildren().clear();

        Map<String, SourceReading> last = services.getSyncService().getLastReadings();
        for (String name : services.getSyncService().getSourceNames()) {
            SourceReading reading = last.get(name);

            Label label = new Label(name);
            label.getStyleClass().add("control-label");
            label.setMinWidth(150.0);

            String statusText;
            if (reading == null) {
                statusText = "not synced yet";
            } else if (reading.isOk()) {
                statusText = "updated in " + reading.getDurationMillis() + " ms";
            } else {
                statusText = reading.getMessage();
            }

            Label status = new Label(statusText);
            status.getStyleClass().add("hint-label");

            HBox row = new HBox(10.0, label, status);
            sourceRows.getChildren().add(row);
        }
    }

    private void refreshSyncButtons() {
        if (services == null) {
            return;
        }
        boolean syncing = services.getSyncService().isSyncing();
        syncNowButton.setDisable(syncing);
        cancelSyncButton.setDisable(!syncing);
        syncNowButton.setText(syncing ? "Syncing…" : "Sync now");
    }

    @FXML
    private void handleSyncNow() {
        if (services == null) {
            return;
        }
        // Every callback below arrives on the sync coordinator thread.
        services.getSyncService().syncAll(new SyncService.Listener() {
            @Override
            public void onSyncStarted(List<String> sourceNames) {
                Platform.runLater(ActivityCenterController.this::refreshSyncButtons);
            }

            @Override
            public void onSourceFinished(SourceReading reading) {
                Platform.runLater(ActivityCenterController.this::refreshSources);
            }

            @Override
            public void onSyncFinished(MergedReading merged, List<SourceReading> readings) {
                Platform.runLater(() -> {
                    refreshSources();
                    refreshSyncButtons();
                });
            }
        });
        refreshSyncButtons();
    }

    @FXML
    private void handleCancelSync() {
        if (services != null) {
            services.getSyncService().cancelSync();
        }
    }

    // ===================== DIAGNOSTICS =====================

    @FXML
    private void handleRunDiagnostics() {
        runDiagnosticsButton.setDisable(true);
        diagnosticsResultLabel.setText("Running…");

        // Must not run on the FX thread: it blocks until every test thread
        // finishes, which would freeze the window.
        Thread runner = new Thread(() -> {
            try {
                IntegrityCheck.Result result = IntegrityCheck.run(4, 200_000);

                String unprotected = result.lostUpdates() == 0
                        ? "matched by luck this run — run it again"
                        : result.lostUpdates() + " updates lost";

                String text = "Expected " + result.expected + " each."
                        + "\nUnprotected tally: " + result.unprotectedTotal + "  (" + unprotected + ")"
                        + "\nGuarded tally: " + result.guardedTotal
                        + (result.guardedIsCorrect() ? "  ✓ correct" : "  ✗ FAILED")
                        + "\nFinished in " + result.millis + " ms.";

                Platform.runLater(() -> {
                    diagnosticsResultLabel.setText(text);
                    runDiagnosticsButton.setDisable(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    diagnosticsResultLabel.setText("Self-test interrupted.");
                    runDiagnosticsButton.setDisable(false);
                });
            }
        }, "integrity-check-runner");
        runner.setDaemon(true);
        runner.start();
    }
}
