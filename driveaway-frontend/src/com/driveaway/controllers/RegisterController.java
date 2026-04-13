package com.driveaway.controllers;

import com.driveaway.services.AuthService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * RegisterController — Fixed.
 * Added goToLogin() method that the RegisterView.fxml "Already have account?" button calls.
 */
public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private javafx.scene.control.PasswordField passwordField;
    @FXML private TextField phoneField;

    private AuthService authService = new AuthService();

    @FXML
    public void handleRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String phone    = phoneField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            System.out.println("[REGISTER] Please fill all required fields.");
            return;
        }

        String response = authService.register(name, email, password, phone);
        System.out.println("[REGISTER] Backend response: " + response);

        // After registering → go to login
        SceneNavigator.load("views/LoginView.fxml");
    }

    // ── Missing method that RegisterView.fxml "Already have account?" calls ──
    @FXML
    public void goToLogin() {
        SceneNavigator.load("views/LoginView.fxml");
    }
}
