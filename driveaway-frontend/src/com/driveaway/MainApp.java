package com.driveaway;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.driveaway.utils.SceneNavigator;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Set stage in SceneNavigator for navigation between scenes
        SceneNavigator.setStage(stage);

        // Load Login page as entry point
        FXMLLoader loader = new FXMLLoader(
            new java.io.File("src/com/driveaway/views/LoginView.fxml")
                .toURI().toURL()
        );

        Scene scene = new Scene(loader.load(), 600, 400);

        // ✅ Load CSS from views/style folder
        scene.getStylesheets().add(
            new java.io.File("src/com/driveaway/views/style/main.css")
                .toURI()
                .toString()
        );
    
        stage.setTitle("🚗 DriveAway - Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}