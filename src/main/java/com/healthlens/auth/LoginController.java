package com.healthlens.auth;

import com.healthlens.Main;
import com.healthlens.guide.GuideController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * CONTROLLER for Login.fxml.
 *
 * Accounts are stored in java.util.prefs.Preferences as
 * "user.<username>.passwordHash" and "user.<username>.email" — this is the
 * same store SignupController writes to, so accounts created via Sign Up
 * work here immediately. On first-ever run, if no accounts exist yet, the
 * original demo account (from credentials.txt) is seeded in automatically
 * so the app still works out of the box for grading/testing.
 *
 * NOTE ON SECURITY: this is still a local, single-machine account store
 * (Preferences lives on this computer only) with hashed-but-unsalted
 * passwords — appropriate for a class project, not for a real product.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button toggleVisibilityButton;
    @FXML private Button loginButton;
    @FXML private Button signupButton;
    @FXML private Label errorLabel;

    private boolean passwordVisible = false;
    private final Preferences prefs = Preferences.userNodeForPackage(Main.class);

    @FXML
    public void initialize() {
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        seedDemoAccountIfNeeded();
    }

    @FXML
    private void handleTogglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        passwordVisibleField.setVisible(passwordVisible);
        passwordVisibleField.setManaged(passwordVisible);
        toggleVisibilityButton.setText(passwordVisible ? "Hide password" : "Show password");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        String storedHash = prefs.get("user." + username + ".passwordHash", null);
        boolean valid = storedHash != null && storedHash.equals(PasswordUtil.hash(password));

        if (!valid) {
            errorLabel.setText("Incorrect username or password. Please try again.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Failed");
            alert.setHeaderText(null);
            alert.setContentText("Incorrect username or password.");
            alert.showAndWait();
            return;
        }

        String email = prefs.get("user." + username + ".email", "");
        goToGuide(username, email);
    }

    @FXML
    private void handleGoToSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthlens/auth/Signup.fxml"));
            Parent signupRoot = loader.load();
            Scene scene = new Scene(signupRoot, 420, 620);
            scene.getStylesheets().add(getClass().getResource("/com/healthlens/styles.css").toExternalForm());
            Stage stage = (Stage) signupButton.getScene().getWindow();
            stage.setTitle("HealthLens — Sign Up");
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** One-time migration: if no accounts exist yet, seed the original demo login from credentials.txt. */
    private void seedDemoAccountIfNeeded() {
        Map<String, String> demo = loadDemoCredentialsFile();
        String demoUsername = demo.get("username");
        String demoPassword = demo.get("password");
        if (demoUsername == null || demoPassword == null) {
            return;
        }
        if (prefs.get("user." + demoUsername + ".passwordHash", null) == null) {
            prefs.put("user." + demoUsername + ".passwordHash", PasswordUtil.hash(demoPassword));
            prefs.put("user." + demoUsername + ".email", "samin@example.com");
        }
    }

    private Map<String, String> loadDemoCredentialsFile() {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = getClass().getResourceAsStream("credentials.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (Exception e) {
            System.out.println("[HealthLens] Could not read credentials.txt: " + e.getMessage());
        }
        return map;
    }

    /** SCENE SWITCH: Login -> Guide, passing username + email along. */
    private void goToGuide(String username, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthlens/guide/Guide.fxml"));
            Parent guideRoot = loader.load();
            GuideController guideController = loader.getController();
            guideController.setSession(username, email);

            Scene guideScene = new Scene(guideRoot, 780, 620);
            guideScene.getStylesheets().add(getClass().getResource("/com/healthlens/styles.css").toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("HealthLens — Getting Started Guide");
            stage.setScene(guideScene);
            stage.setMinWidth(700);
            stage.setMinHeight(560);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
