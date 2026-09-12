package com.healthlens;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point for the HealthLens application.
 *
 * The app now starts at the Login screen (com/healthlens/auth/Login.fxml).
 * A successful login switches the Stage's scene to the Guide
 * (com/healthlens/guide/Guide.fxml), and finishing the Guide switches it
 * again to the main dashboard (HealthLens.fxml). All three screens share
 * one Stage — this is the classic "Scene Switching" pattern, done for real
 * across three screens instead of two.
 *
 * Want to skip straight to the dashboard while developing? Change the
 * getResource(...) line below to load "HealthLens.fxml" directly.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthlens/auth/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 420, 500);
        scene.getStylesheets().add(getClass().getResource("/com/healthlens/styles.css").toExternalForm());

        stage.setTitle("HealthLens — Log In");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
