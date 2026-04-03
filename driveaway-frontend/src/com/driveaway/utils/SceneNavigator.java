package com.driveaway.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {

    private static Stage stage;

    public static void setStage(Stage s) {
        stage = s;
    }

    public static void load(String fxml) {
        try {
            Parent root = FXMLLoader.load(
                    SceneNavigator.class.getResource("/com/driveaway/" + fxml)
            );
            // Keep current window size, or use appropriate minimum sizes
            double width = Math.max(stage.getWidth(), 1000);
            double height = Math.max(stage.getHeight(), 680);

            // For login/register/license pages, use a smaller size
            if (fxml.contains("Login") || fxml.contains("Register") || fxml.contains("License")) {
                width = Math.max(stage.getWidth(), 600);
                height = Math.max(stage.getHeight(), 600);
            }

            stage.setScene(new Scene(root, width, height));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
