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
    private static String userRole;   // ✅ NEW

    @FXML
    public void handleLogin() {

        String email = emailField != null ? emailField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (email.isBlank()) {
            setStatus("Please enter your email.");
            return;
        }

        if (password.isBlank()) {
            setStatus("Please enter your password.");
            return;
        }
    

        setStatus("Signing in...");

        new Thread(() -> {

            String response = authService.login(email, password);

            if (response != null && response.contains("id")) {

                userId = extractField(response, "id");
                userName = extractField(response, "name");
                userEmail = extractField(response, "email");
                userPhone = extractField(response, "phone");
                userRole = extractField(response, "role");

                System.out.println("Login Success: " + userId);
                System.out.println("Role: " + userRole);
                System.out.println("RAW RESPONSE: " + response);

                javafx.application.Platform.runLater(() -> {

                    if ("STAFF".equalsIgnoreCase(userRole)) {

                        SceneNavigator.load("views/StaffView.fxml");

                    } else {

                       SceneNavigator.load("views/LicenseView.fxml");
                    }

            });

        } else {

            javafx.application.Platform.runLater(() ->
                    setStatus("Invalid email or password. Please try again.")
            );
        }

    }).start();
}

    @FXML
    public void goToRegister() {
        SceneNavigator.load("views/RegisterView.fxml");
    }

    public static String getUserId() { return userId; }
    public static String getUserName() { return userName; }
    public static String getUserEmail() { return userEmail; }
    public static String getUserPhone() { return userPhone; }
    public static String getUserRole() { return userRole; } // ✅ NEW

    public static void setUserName(String name) { userName = name; }

    public static void logout() {
        userId = null;
        userName = null;
        userEmail = null;
        userPhone = null;
        userRole = null; // ✅ NEW
    }

    private void setStatus(String msg) {
        if (statusLabel != null)
            statusLabel.setText(msg);
    }

    private String extractField(String json, String field) {

        try {

             String key = "\"" + field + "\"";

            int keyIndex = json.indexOf(key);

            if (keyIndex == -1) return null;

            int colonIndex = json.indexOf(":", keyIndex);

            int startQuote = json.indexOf("\"", colonIndex + 1);

            int endQuote = json.indexOf("\"", startQuote + 1);

            return json.substring(startQuote + 1, endQuote);

        } catch (Exception e) {

            return null;
    }
}
    
}