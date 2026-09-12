package com.healthlens.guide;

import com.healthlens.HealthLensController;
import com.healthlens.model.Person;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CONTROLLER for Guide.fxml.
 *
 * This class deliberately implements javafx.fxml.Initializable (rather than
 * the @FXML initialize() shortcut used elsewhere in the project) to
 * demonstrate that pattern explicitly. It hosts a 10-page "wizard": one page
 * is visible at a time inside pageStack, and Back/Next swap which page is
 * shown. Each page demonstrates a specific JavaFX control.
 */
public class GuideController implements Initializable {

    // --- navigation ---
    @FXML private VBox page1, page2, page3, page4, page5, page6, page7, page8, page9, page10;
    @FXML private Label stepLabel;
    @FXML private Button backButton;
    @FXML private Button nextButton;

    // --- page 1 ---
    @FXML private Label welcomeLabel;

    // --- page 2 ---
    @FXML private ToggleGroup experienceGroup;
    @FXML private RadioButton beginnerRadio;
    @FXML private RadioButton intermediateRadio;
    @FXML private RadioButton expertRadio;
    @FXML private Label experienceResultLabel;
    @FXML private CheckBox sleepCheck, waterCheck, exerciseCheck, stressCheck, moodCheck;
    @FXML private Label interestsResultLabel;

    // --- page 3 ---
    @FXML private ComboBox<String> countryCombo;
    @FXML private ColorPicker accentColorPicker;
    @FXML private Label colorDemoLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private Label startDateResultLabel;

    // --- page 4 ---
    @FXML private ListView<String> tipsListView;
    @FXML private Label tipDetailLabel;

    // --- page 5 ---
    @FXML private TreeView<String> featureTreeView;
    @FXML private Label treeSelectionLabel;

    // --- page 6 ---
    @FXML private TableView<Person> personTable;
    @FXML private TableColumn<Person, String> nameColumn;
    @FXML private TableColumn<Person, String> tipColumn;
    @FXML private TableColumn<Person, Integer> scoreColumn;

    // --- page 7 ---
    @FXML private TextArea goalTextArea;

    // --- page 8 ---
    @FXML private Label fontDemoLabel;
    @FXML private Slider fontSizeSlider;

    // --- page 9 ---
    @FXML private TextField targetField;
    @FXML private Label currentSumLabel;
    @FXML private ProgressBar accumulateProgressBar;
    private int currentSum = 0;

    // --- page 10 ---
    @FXML private ImageView profileImageView;

    private List<VBox> pages;
    private int currentPageIndex = 0;
    private String userName;

    /** Called by LoginController right after loading this FXML. */
    public void setUserName(String userName) {
        this.userName = userName;
        if (userName != null && !userName.isBlank()) {
            welcomeLabel.setText("Welcome, " + userName + "!");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pages = new ArrayList<>(List.of(page1, page2, page3, page4, page5, page6, page7, page8, page9, page10));
        showPage(0);

        setupExperienceRadios();
        setupPreferences();
        setupTips();
        setupFeatureTree();
        setupPersonTable();
        setupFontSlider();
        setupAccumulate();
    }

    // ===================== WIZARD NAVIGATION =====================

    private void showPage(int index) {
        for (int i = 0; i < pages.size(); i++) {
            boolean show = (i == index);
            pages.get(i).setVisible(show);
            pages.get(i).setManaged(show);
        }
        currentPageIndex = index;
        stepLabel.setText("Step " + (index + 1) + " of " + pages.size());
        backButton.setDisable(index == 0);
        nextButton.setText(index == pages.size() - 1 ? "Finish" : "Next →");
    }

    @FXML
    private void handleBack() {
        if (currentPageIndex > 0) {
            showPage(currentPageIndex - 1);
        }
    }

    @FXML
    private void handleNext() {
        if (currentPageIndex == 6 && (goalTextArea.getText() == null || goalTextArea.getText().isBlank())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No goal entered");
            alert.setHeaderText(null);
            alert.setContentText("You haven't written a goal yet — you can still continue, or go back and add one.");
            alert.showAndWait();
        }

        if (currentPageIndex < pages.size() - 1) {
            showPage(currentPageIndex + 1);
        } else {
            handleFinish();
        }
    }

    private void handleFinish() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("All set");
        alert.setHeaderText(null);
        alert.setContentText("Continue to HealthLens?");
        alert.showAndWait().ifPresent(result -> {
            if (result.getButtonData().isDefaultButton()) {
                goToHealthLens();
            }
        });
    }

    /** SCENE SWITCH #2: Guide -> HealthLens, passing the username along. */
    private void goToHealthLens() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthlens/HealthLens.fxml"));
            Parent healthLensRoot = loader.load();
            HealthLensController controller = loader.getController();
            controller.setWelcomeMessage(userName);

            Scene scene = new Scene(healthLensRoot, 1050, 700);
            scene.getStylesheets().add(getClass().getResource("/com/healthlens/styles.css").toExternalForm());

            Stage stage = (Stage) nextButton.getScene().getWindow();
            stage.setTitle("HealthLens — Daily Health Dashboard");
            stage.setScene(scene);
            stage.setMinWidth(950);
            stage.setMinHeight(650);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== MENU BAR =====================

    @FXML
    private void handleMenuRestart() {
        showPage(0);
    }

    @FXML
    private void handleMenuAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About HealthLens");
        alert.setHeaderText(null);
        alert.setContentText("HealthLens is a JavaFX health dashboard for tracking sleep, water, "
                + "exercise, mood, and stress. This guide walks you through the app before you dive in.");
        alert.showAndWait();
    }

    @FXML
    private void handleMenuExit() {
        Platform.exit();
    }

    // ===================== PAGE 1: ALERT TYPE BUTTONS =====================

    @FXML
    private void handleShowInfoAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText("This is an Information alert — used for friendly, non-blocking messages.");
        alert.showAndWait();
    }

    @FXML
    private void handleShowWarningAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText("This is a Warning alert — used to flag something worth double-checking.");
        alert.showAndWait();
    }

    @FXML
    private void handleShowErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText("This is an Error alert — used when something has actually gone wrong.");
        alert.showAndWait();
    }

    // ===================== PAGE 2: RADIO + TOGGLEGROUP + CHECKBOX =====================

    private void setupExperienceRadios() {
        experienceGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == beginnerRadio) {
                experienceResultLabel.setText("Selected: Beginner");
            } else if (newToggle == intermediateRadio) {
                experienceResultLabel.setText("Selected: Intermediate");
            } else if (newToggle == expertRadio) {
                experienceResultLabel.setText("Selected: Expert");
            }
        });
    }

    @FXML
    private void handleInterestsSubmit() {
        List<String> chosen = new ArrayList<>();
        if (sleepCheck.isSelected()) chosen.add("Sleep");
        if (waterCheck.isSelected()) chosen.add("Water");
        if (exerciseCheck.isSelected()) chosen.add("Exercise");
        if (stressCheck.isSelected()) chosen.add("Stress");
        if (moodCheck.isSelected()) chosen.add("Mood");

        interestsResultLabel.setText(chosen.isEmpty()
                ? "You didn't select anything — that's okay, you can track everything in HealthLens anyway."
                : "You want to track: " + String.join(", ", chosen));
    }

    // ===================== PAGE 3: COMBOBOX + COLORPICKER + DATEPICKER =====================

    private void setupPreferences() {
        countryCombo.setItems(FXCollections.observableArrayList(
                "Bangladesh", "United States", "United Kingdom", "Canada", "Other"));

        accentColorPicker.setOnAction(e -> {
            javafx.scene.paint.Color c = accentColorPicker.getValue();
            colorDemoLabel.setTextFill(c);
        });

        startDatePicker.setOnAction(e -> {
            if (startDatePicker.getValue() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                startDateResultLabel.setText("Your journey starts on " + startDatePicker.getValue().format(formatter));
            }
        });
    }

    // ===================== PAGE 4: LISTVIEW =====================

    private void setupTips() {
        tipsListView.setItems(FXCollections.observableArrayList(
                "Drink a glass of water right after waking up",
                "Keep a consistent sleep schedule, even on weekends",
                "Take a short walk after meals",
                "Try a 2-minute breathing exercise when stressed",
                "Write down one thing you're grateful for each day"
        ));
        tipsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                tipDetailLabel.setText("Tip: " + newV);
            }
        });
    }

    // ===================== PAGE 5: TREEVIEW =====================

    private void setupFeatureTree() {
        TreeItem<String> root = new TreeItem<>("HealthLens Features");
        root.setExpanded(true);

        TreeItem<String> sleep = new TreeItem<>("Sleep Tracking");
        sleep.getChildren().addAll(new TreeItem<>("Slider input"), new TreeItem<>("Progress bar vs. goal"));

        TreeItem<String> water = new TreeItem<>("Water Tracking");
        water.getChildren().addAll(new TreeItem<>("Slider input"), new TreeItem<>("Progress bar vs. goal"));

        TreeItem<String> exercise = new TreeItem<>("Exercise Tracking");
        exercise.getChildren().addAll(new TreeItem<>("Slider input"), new TreeItem<>("Progress bar vs. goal"));

        TreeItem<String> stress = new TreeItem<>("Stress Tracking");
        stress.getChildren().addAll(new TreeItem<>("Slider input"), new TreeItem<>("Inverted scoring"));

        TreeItem<String> mood = new TreeItem<>("Mood");
        mood.getChildren().add(new TreeItem<>("Choice box selection"));

        root.getChildren().addAll(sleep, water, exercise, stress, mood);
        featureTreeView.setRoot(root);

        featureTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                treeSelectionLabel.setText("Selected: " + newV.getValue());
            }
        });
    }

    // ===================== PAGE 6: TABLEVIEW + PERSON + OBSERVABLELIST =====================

    private void setupPersonTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        tipColumn.setCellValueFactory(new PropertyValueFactory<>("healthTip"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("weeklyScore"));

        ObservableList<Person> people = FXCollections.observableArrayList(
                new Person("Amina", "Drinks 8 glasses of water daily", 92),
                new Person("Rafi", "Walks 30 minutes every morning", 78),
                new Person("Nadia", "Sleeps 8 hours on a fixed schedule", 88)
        );
        personTable.setItems(people);
    }

    // ===================== PAGE 8: SLIDER =====================

    private void setupFontSlider() {
        fontSizeSlider.valueProperty().addListener((obs, oldV, newV) ->
                fontDemoLabel.setStyle("-fx-font-size: " + newV.intValue() + "px;"));
    }

    // ===================== PAGE 9: PROGRESSBAR ACCUMULATE =====================

    private void setupAccumulate() {
        currentSum = 0;
    }

    @FXML
    private void handleAccumulate() {
        int target;
        try {
            target = Integer.parseInt(targetField.getText().trim());
        } catch (Exception e) {
            currentSumLabel.setText("Enter a valid number first.");
            return;
        }
        if (target <= 0) {
            currentSumLabel.setText("Target must be greater than 0.");
            return;
        }
        currentSum++;
        currentSumLabel.setText("Current sum: " + currentSum);
        accumulateProgressBar.setProgress(Math.min(1.0, (double) currentSum / target));
    }

    @FXML
    private void handleResetAccumulate() {
        currentSum = 0;
        currentSumLabel.setText("Current sum: 0");
        accumulateProgressBar.setProgress(0);
    }

    // ===================== PAGE 7: TEXTAREA =====================

    @FXML
    private void handleClearGoal() {
        goalTextArea.clear();
    }

    // ===================== PAGE 10: FILECHOOSER + IMAGEVIEW =====================

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a profile picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        Stage stage = (Stage) profileImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            profileImageView.setImage(new Image(file.toURI().toString()));
        }
    }
}
