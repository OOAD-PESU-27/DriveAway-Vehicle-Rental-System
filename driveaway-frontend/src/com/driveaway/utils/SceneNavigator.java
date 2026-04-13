package com.driveaway.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * SceneNavigator — Fixed version.
 *
 * KEY FIX: Added proper error handling with an Alert dialog.
 * Before: if FXML loading failed, the app just crashed silently.
 * Now: a popup tells you EXACTLY which file failed and why.
 *
 * This is critical for debugging FXML issues during demo.
 */
public class SceneNavigator {

    private static Stage stage;

    public static void setStage(Stage s) {
        stage = s;
    }

    public static void load(String fxml) {
        try {
            String path = "/com/driveaway/" + fxml;
            System.out.println("[NAVIGATOR] Loading: " + path);

            java.net.URL url = SceneNavigator.class.getResource(path);
            if (url == null) {
                showError("FXML file not found on classpath:\n" + path
                        + "\n\nMake sure the file exists in target/classes" + path);
                return;
            }

            Parent root = FXMLLoader.load(url);
            stage.setScene(new Scene(root));
            stage.show();

            System.out.println("[NAVIGATOR] Loaded successfully: " + fxml);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load screen: " + fxml
                    + "\n\nError: " + e.getMessage()
                    + "\n\nCheck console for full stack trace.");
        }
    }

    /**
     * Shows a popup error dialog instead of silent crash.
     * During demo, if something breaks, you'll see EXACTLY what it is.
     */
    private static void showError(String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not load screen");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception ignored) {
            System.err.println("[NAVIGATOR ERROR] " + message);
        }
    }
}
