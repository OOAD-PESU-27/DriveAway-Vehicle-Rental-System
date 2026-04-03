package com.driveaway.controllers;

import com.driveaway.services.AuthService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private Label statusLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleRegister() {
        String name = nameField != null ? nameField.getText() : "";
        String email = emailField != null ? emailField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";
        String phone = phoneField != null ? phoneField.getText() : "";

        if (name.isBlank()) { setStatus("Full name is required."); return; }
        if (email.isBlank()) { setStatus("Email address is required."); return; }
        if (password.isBlank() || password.length() < 6) {
            setStatus("Password must be at least 6 characters."); return;
        }
        if (phone.isBlank()) { setStatus("Phone number is required."); return; }

        setStatus("Creating your account...");
        String response = authService.register(name, email, password, phone);

        if (response != null && (response.contains("id") || response.contains("success"))) {
            setStatus("✅ Account created! Please sign in.");
            SceneNavigator.load("views/LoginView.fxml");
        } else {
            setStatus("Registration failed. Email may already be in use.");
        }
    }

    @FXML
    public void goToLogin() {
        SceneNavigator.load("views/LoginView.fxml");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }
}
