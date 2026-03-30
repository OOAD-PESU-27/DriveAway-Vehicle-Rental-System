package com.driveaway.controllers;

import com.driveaway.services.AuthService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField;

    @FXML
    private TextField phoneField;

    private AuthService authService = new AuthService();

    @FXML
    public void handleRegister() {

        String response = authService.register(
                nameField.getText(),
                emailField.getText(),
                passwordField.getText(),
                phoneField.getText()
        );

        System.out.println(response);

        // after register → go to login
        SceneNavigator.load("views/LoginView.fxml");
    }
}