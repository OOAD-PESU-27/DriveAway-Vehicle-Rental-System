package com.driveaway;

import com.driveaway.utils.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * MainApp — JavaFX entry point.
 *
 * YOUR PROBLEM WAS: MainApp.java was completely empty!
 * The decompiled class showed no start() method at all.
 * JavaFX needs start(Stage) to launch — without it, nothing runs.
 *
 * FIX: Implement Application properly, set the stage in SceneNavigator,
 * then load LoginView as the first screen.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Give SceneNavigator a reference to the window
        SceneNavigator.setStage(primaryStage);

        primaryStage.setTitle("DriveAway Vehicle Rental System");
        primaryStage.setWidth(700);
        primaryStage.setHeight(550);
        primaryStage.setResizable(false);

        // Start at Login screen
        SceneNavigator.load("views/LoginView.fxml");

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
