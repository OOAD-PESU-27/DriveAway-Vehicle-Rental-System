package com.driveaway.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {

    private static final double MAIN_WIDTH = 1000;
    private static final double MAIN_HEIGHT = 680;
    private static final double AUTH_WIDTH = 600;
    private static final double AUTH_HEIGHT = 600;

    private static Stage stage;

    public static void setStage(Stage s) {
        stage = s;
    }

    public static void load(String fxml) {
        try {
            Parent root = FXMLLoader.load(
                    SceneNavigator.class.getResource("/com/driveaway/" + fxml)
            );
            // Use smaller window for authentication/license pages
            boolean isAuthPage = fxml.contains("Login") || fxml.contains("Register")
                    || fxml.contains("License");
            double width = isAuthPage
                    ? Math.max(stage.getWidth(), AUTH_WIDTH)
                    : Math.max(stage.getWidth(), MAIN_WIDTH);
            double height = isAuthPage
                    ? Math.max(stage.getHeight(), AUTH_HEIGHT)
                    : Math.max(stage.getHeight(), MAIN_HEIGHT);

            stage.setScene(new Scene(root, width, height));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
