package com.driveaway;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Load FXML from src folder
        FXMLLoader loader = new FXMLLoader(
            new java.io.File("src/com/driveaway/views/VehicleListView.fxml")
                .toURI().toURL()
        );

        Scene scene = new Scene(loader.load(), 400, 500);

        // ✅ Load CSS from views/style folder
        scene.getStylesheets().add(
            new java.io.File("src/com/driveaway/views/style/main.css")
                .toURI()
                .toString()
        );
    
        stage.setTitle("🚗 DriveAway Vehicles");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}