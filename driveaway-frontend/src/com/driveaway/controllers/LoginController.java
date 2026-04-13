package com.driveaway.controllers;

import com.driveaway.services.AuthService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * LoginController — Updated to route CUSTOMER vs STAFF to different screens.
 *
 * ROLE ROUTING LOGIC:
 *   Backend returns User JSON with a "role" field:
 *     {"id":"...","name":"...","email":"...","role":"CUSTOMER",...}
 *     {"id":"...","name":"...","email":"...","role":"STAFF",...}
 *
 *   CUSTOMER → BookingView.fxml  (existing booking screen)
 *   STAFF    → StaffView.fxml    (new staff-only dashboard)
 *   ADMIN    → StaffView.fxml    (same as staff for now)
 *
 * Nothing else changed — all existing logic preserved.
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    // ── Shared session state ──────────────────────────────────────────────────
    private static String userId    = "69daa151cde446d5c530c5cb"; // Sushma's real ID
    private static String userEmail = "";
    private static String userRole  = "CUSTOMER";  // default
    private static String userName  = "";

    private AuthService authService = new AuthService();

    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        userEmail = email;

        try {
            String response = authService.login(email, password);
            System.out.println("[LOGIN] Response: " + response);

            if (response != null && (response.contains("\"id\"") || response.contains("\"userId\""))) {
                // Extract id from backend response, accepting either field name.
                userId = extractField(response, "id");
                if (userId == null || userId.isEmpty()) {
                    userId = extractField(response, "userId");
                }
                userName = extractField(response, "name");
                userRole = extractField(response, "role");

                // Fallback if role not in response
                if (userRole == null || userRole.isEmpty()) {
                    userRole = "CUSTOMER";
                }

                System.out.println("[LOGIN] userId=" + userId + " role=" + userRole + " name=" + userName);

                clearError();

                // ── ROLE-BASED ROUTING ────────────────────────────────────
                if ("STAFF".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
                    SceneNavigator.load("views/StaffView.fxml");
                } else {
                    SceneNavigator.load("views/BookingView.fxml");
                }

            } else if (response == null) {
                // Backend unreachable → demo mode, still route by email pattern
                System.out.println("[LOGIN] Backend offline — demo mode");
                showError("Backend offline — demo mode");
                demoRoute(email);

            } else {
                // Wrong credentials
                showError("Invalid email or password. Response: " + response);
            }

        } catch (Exception e) {
            System.out.println("[LOGIN] Exception: " + e.getMessage());
            showError("Cannot reach backend. Demo mode.");
            demoRoute(email);
        }
    }

    /**
     * Demo routing when backend is down — checks email to decide screen.
     * staff@driveaway.com → StaffView
     * anything else → BookingView
     */
    private void demoRoute(String email) {
        if (email.toLowerCase().contains("staff") || email.toLowerCase().contains("admin")) {
            userRole = "STAFF";
            SceneNavigator.load("views/StaffView.fxml");
        } else {
            userRole = "CUSTOMER";
            SceneNavigator.load("views/BookingView.fxml");
        }
    }

    @FXML
    public void goToRegister() {
        SceneNavigator.load("views/RegisterView.fxml");
    }

    // ── Static getters/setters ────────────────────────────────────────────────
    public static String getUserId()    { return userId; }
    public static String getUserEmail() { return userEmail; }
    public static String getUserRole()  { return userRole; }
    public static String getUserName()  { return userName.isEmpty() ? userEmail : userName; }
    public static void   setUserId(String id) { userId = id; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String extractField(String json, String field) {
        try {
            String key = "\"" + field + "\":\"";
            int start = json.indexOf(key);
            if (start < 0) return "";
            start += key.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) { return ""; }
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
            errorLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 12px;");
        }
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }
}
