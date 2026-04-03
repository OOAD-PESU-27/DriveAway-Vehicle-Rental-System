package com.driveaway.controllers;

import com.driveaway.services.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import com.driveaway.utils.SceneNavigator;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final AuthService authService = new AuthService();

    // Session data
    private static String userId;
    private static String userName;
    private static String userEmail;
    private static String userPhone;

    @FXML
    public void handleLogin() {
        String email = emailField != null ? emailField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (email.isBlank()) { setStatus("Please enter your email."); return; }
        if (password.isBlank()) { setStatus("Please enter your password."); return; }

        setStatus("Signing in...");
        String response = authService.login(email, password);

        if (response != null && response.contains("id")) {
            userId = extractField(response, "id");
            userName = extractField(response, "name");
            userEmail = extractField(response, "email");
            userPhone = extractField(response, "phone");
            System.out.println("Login Success: " + userId);
            SceneNavigator.load("views/LicenseView.fxml");
        } else {
            setStatus("Invalid email or password. Please try again.");
        }
    }

    @FXML
    public void goToRegister() {
        SceneNavigator.load("views/RegisterView.fxml");
    }

    public static String getUserId() { return userId; }
    public static String getUserName() { return userName; }
    public static String getUserEmail() { return userEmail; }
    public static String getUserPhone() { return userPhone; }

    public static void setUserName(String name) { userName = name; }

    public static void logout() {
        userId = null;
        userName = null;
        userEmail = null;
        userPhone = null;
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        if (start >= json.length()) return null;
        char ch = json.charAt(start);
        if (ch == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        }
        return null;
    }
}
