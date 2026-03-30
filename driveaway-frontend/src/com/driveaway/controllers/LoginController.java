package com.driveaway.controllers;

import com.driveaway.services.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import com.driveaway.utils.SceneNavigator;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField;

    private AuthService authService = new AuthService();

    private static String userId; // session

    @FXML
    public void handleLogin() {

        String response = authService.login(
                emailField.getText(),
                passwordField.getText()
        );

        if (response != null && response.contains("id")) {

            userId = response.split("\"id\":\"")[1].split("\"")[0];

            System.out.println("Login Success: " + userId);

            // 👉 Open License Page
            SceneNavigator.load("views/LicenseView.fxml");

        } else {
            System.out.println("Login Failed");
        }
    }

    public static String getUserId() {
        return userId;
    }
    @FXML
public void goToRegister() {
    SceneNavigator.load("views/RegisterView.fxml");
}
}