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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * CONTROLLER for Signup.fxml.
 *
 * Flow: fill in details -> "Send Verification Code" generates a 6-digit
 * code and shows it directly in a popup (this is a class-project "email
 * verification" demo, not a real email send — no SMTP setup needed).
 * Entering the correct code creates the account (username/passwordHash/
 * email saved to Preferences) and logs the user straight in, same as a
 * normal login would.
 */
public class SignupController {

    @FXML private VBox detailsSection;
    @FXML private VBox verifySection;

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button sendCodeButton;

    @FXML private TextField codeField;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;

    @FXML private Label statusLabel;
    @FXML private Button backToLoginButton;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Preferences prefs = Preferences.userNodeForPackage(Main.class);

    private String pendingUsername;
    private String pendingEmail;
    private String pendingPasswordHash;
    private String pendingCode;

    @FXML
    private void handleSendCode() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            showStatus("Please fill in every field.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showStatus("That doesn't look like a valid email address.");
            return;
        }
        if (password.length() < 6) {
            showStatus("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showStatus("Passwords don't match.");
            return;
        }
        if (prefs.get("user." + username + ".passwordHash", null) != null) {
            showStatus("That username is already taken.");
            return;
        }

        pendingUsername = username;
        pendingEmail = email;
        pendingPasswordHash = PasswordUtil.hash(password);
        pendingCode = String.format("%06d", (int) (Math.random() * 1_000_000));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Verification code");
        alert.setHeaderText(null);
        alert.setContentText("Your verification code is:\n\n" + pendingCode);
        alert.showAndWait();

        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        verifySection.setVisible(true);
        verifySection.setManaged(true);
    }

    @FXML
    private void handleVerifyCode() {
        String entered = codeField.getText() == null ? "" : codeField.getText().trim();
        if (pendingCode == null) {
            showStatus("Request a code first.");
            return;
        }
        if (!entered.equals(pendingCode)) {
            showStatus("Incorrect code. Please try again.");
            return;
        }

        prefs.put("user." + pendingUsername + ".passwordHash", pendingPasswordHash);
        prefs.put("user." + pendingUsername + ".email", pendingEmail);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account created");
        alert.setHeaderText(null);
        alert.setContentText("Welcome, " + pendingUsername + "! Taking you to the guide now.");
        alert.showAndWait();

        goToGuide(pendingUsername, pendingEmail);
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthlens/auth/Login.fxml"));
            Parent loginRoot = loader.load();
            Scene scene = new Scene(loginRoot, 420, 500);
            scene.getStylesheets().add(getClass().getResource("/com/healthlens/styles.css").toExternalForm());
            Stage stage = (Stage) backToLoginButton.getScene().getWindow();
            stage.setTitle("HealthLens — Log In");
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToGuide(String username, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthlens/guide/Guide.fxml"));
            Parent guideRoot = loader.load();
            GuideController guideController = loader.getController();
            guideController.setSession(username, email);

            Scene guideScene = new Scene(guideRoot, 780, 620);
            guideScene.getStylesheets().add(getClass().getResource("/com/healthlens/styles.css").toExternalForm());

            Stage stage = (Stage) verifyButton.getScene().getWindow();
            stage.setTitle("HealthLens — Getting Started Guide");
            stage.setScene(guideScene);
            stage.setMinWidth(700);
            stage.setMinHeight(560);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
