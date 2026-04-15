package com.driveaway.controllers;

import com.driveaway.services.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import com.driveaway.utils.SceneNavigator;
import java.util.prefs.Preferences; // 🔥 IMPORT DEMO MAGIC MEMORY

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
    private static String userRole;
    private static String userLicenseId; 

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
                userLicenseId = extractField(response, "licenseId"); 

                System.out.println("Login Success: " + userId);
                System.out.println("Role: " + userRole);
                System.out.println("Backend License ID: " + userLicenseId);

                javafx.application.Platform.runLater(() -> {
                    
                    if ("admin@gmail.com".equalsIgnoreCase(email)) {
                        SceneNavigator.load("views/AdminDashboardView.fxml");
                        
                    } else if ("STAFF".equalsIgnoreCase(userRole)) {
                        SceneNavigator.load("views/StaffView.fxml");
                        
                    } else {
                        // 🔥 DEMO MAGIC ROUTER: Check the backend AND our local PC memory!
                        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
                        boolean hasLocalLicense = prefs.getBoolean("has_license_" + userId, false);

                        if ((userLicenseId == null || userLicenseId.isBlank() || "null".equalsIgnoreCase(userLicenseId)) && !hasLocalLicense) {
                            System.out.println("No license found anywhere! Routing to License view...");
                            SceneNavigator.load("views/LicenseView.fxml");
                        } else {
                            System.out.println("Valid license confirmed! Skipping straight to Dashboard...");
                            
                            // If backend actually sent a real ID this time, save it to memory so we never forget
                            if (userLicenseId != null && !userLicenseId.isBlank() && !"null".equalsIgnoreCase(userLicenseId)) {
                                prefs.putBoolean("has_license_" + userId, true);
                            }
                            
                            SceneNavigator.load("views/DashboardView.fxml"); // Or VehicleCatalogView.fxml
                        }
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
    public static String getUserRole() { return userRole; }
    public static String getUserLicenseId() { return userLicenseId; } 

    public static void setUserName(String name) { userName = name; }

    public static void logout() {
        userId = null;
        userName = null;
        userEmail = null;
        userPhone = null;
        userRole = null;
        userLicenseId = null;
    }

    private void setStatus(String msg) {
        if (statusLabel != null)
            statusLabel.setText(msg);
    }

    private String extractField(String json, String field) {
        if (json == null) return null;
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
        
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        String val = end > start ? json.substring(start, end).trim() : null;
        return "null".equals(val) ? null : val;
    }
}