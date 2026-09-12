package com.healthlens.auth;

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

/**
 * CONTROLLER for Login.fxml.
 *
 * Demonstrates: PasswordField (+ a "show password" toggle built from an
 * overlapping TextField), TextField Enter-key handling, Alert dialogs
 * (Error type on bad login), and Scene Switching (on success, swaps the
 * Stage's scene to Guide.fxml and passes the username across).
 *
 * NOTE ON SECURITY: credentials.txt is a plain-text file bundled into the
 * app's resources. That's fine for a class assignment demo, but it is NOT
 * how real authentication should work — anyone with the built app could
 * open the jar and read the password. A real app would check credentials
 * against a server, and never ship the correct password inside the client.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button toggleVisibilityButton;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        // Keep the hidden and visible password fields in sync with each other.
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
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

        Map<String, String> credentials = loadCredentials();
        boolean valid = username.equals(credentials.get("username")) && password.equals(credentials.get("password"));

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

        goToGuide(username);
    }

    /** Reads username=... / password=... lines out of credentials.txt. */
    private Map<String, String> loadCredentials() {
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

    /** SCENE SWITCH #1: Login -> Guide, passing the username along. */
    private void goToGuide(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthlens/guide/Guide.fxml"));
            Parent guideRoot = loader.load();
            GuideController guideController = loader.getController();
            guideController.setUserName(username);

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
