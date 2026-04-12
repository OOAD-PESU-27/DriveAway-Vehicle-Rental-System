package com.driveaway;

import com.driveaway.utils.SceneNavigator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/driveaway/views/LoginView.fxml")
        );

        Scene scene = new Scene(loader.load(), 1000, 680);

        // Set stage for navigation
        SceneNavigator.setStage(stage);

        stage.setTitle("DriveAway - Vehicle Rental System");
        stage.setMinWidth(800);
        stage.setMinHeight(580);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}