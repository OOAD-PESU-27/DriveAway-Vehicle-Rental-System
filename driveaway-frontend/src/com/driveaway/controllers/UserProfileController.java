package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.utils.HttpUtil;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * UserProfileController - Manages the user profile page
 * Displays user info, license details, and booking summary
 */
public class UserProfileController {

    @FXML private Label avatarLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label userIdLabel;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField licenseNumberField;
    @FXML private TextField licenseExpiryField;
    @FXML private Label totalBookingsLabel;
    @FXML private Label activeBookingsLabel;
    @FXML private Label completedBookingsLabel;
    @FXML private Button editToggleBtn;
    @FXML private HBox saveSection;
    @FXML private Label saveStatusLabel;

    private static final String BASE_URL = "http://localhost:8080";

    private final BookingService bookingService = new BookingService();
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        String userId = LoginController.getUserId();
        String userName = LoginController.getUserName();
        String userEmail = LoginController.getUserEmail();

        // Attempt to load fresh profile data from backend
        if (userId != null) {
            loadProfileFromBackend(userId);
            loadBookingStats(userId);
        } else {
            // Fall back to session cache
            populateFromSession(userName, userEmail);
        }
    }

    private void loadProfileFromBackend(String userId) {
        try {
            String response = HttpUtil.sendGet(BASE_URL + "/user/" + userId);
            if (response != null && response.contains("\"id\"")) {
                String name = extractField(response, "name");
                String email = extractField(response, "email");
                String phone = extractField(response, "phone");

                // Update session cache
                if (name != null) LoginController.setUserName(name);

                populateFromSession(
                        name != null ? name : LoginController.getUserName(),
                        email != null ? email : LoginController.getUserEmail()
                );
                if (phoneField != null && phone != null) phoneField.setText(phone);
                if (userIdLabel != null) userIdLabel.setText("ID: " + shorten(userId, 20));
            } else {
                // Backend unavailable – use session data
                populateFromSession(LoginController.getUserName(), LoginController.getUserEmail());
            }
        } catch (Exception e) {
            populateFromSession(LoginController.getUserName(), LoginController.getUserEmail());
        }
    }

    private void populateFromSession(String userName, String userEmail) {
        String userId = LoginController.getUserId();
        if (userIdLabel != null) userIdLabel.setText("ID: " + (userId != null ? shorten(userId, 20) : "N/A"));
        if (profileNameLabel != null) profileNameLabel.setText(userName != null ? userName : "User");
        if (profileEmailLabel != null) profileEmailLabel.setText(userEmail != null ? userEmail : "");
        if (avatarLabel != null) {
            String initials = (userName != null && !userName.isBlank())
                    ? String.valueOf(userName.charAt(0)).toUpperCase() : "U";
            avatarLabel.setText(initials);
        }
        if (nameField != null) nameField.setText(userName != null ? userName : "");
        if (emailField != null) emailField.setText(userEmail != null ? userEmail : "");
        if (phoneField != null) phoneField.setText(LoginController.getUserPhone() != null
                ? LoginController.getUserPhone() : "");
        if (licenseNumberField != null) licenseNumberField.setText("Verified ✓");
        if (licenseExpiryField != null) licenseExpiryField.setText("On file");
    }

    private void loadBookingStats(String userId) {
        String response = bookingService.getUserBookings(userId);
        if (response == null || response.isBlank() || response.equals("[]")) {
            if (totalBookingsLabel != null) totalBookingsLabel.setText("0");
            if (activeBookingsLabel != null) activeBookingsLabel.setText("0");
            if (completedBookingsLabel != null) completedBookingsLabel.setText("0");
            return;
        }

        String[] entries = response.replace("[", "").replace("]", "").split("\\},\\{");
        int total = 0, active = 0, completed = 0;
        for (String entry : entries) {
            String status = extractField(entry, "status");
            if (status != null) {
                total++;
                if ("ACTIVE".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status)) active++;
                if ("COMPLETED".equalsIgnoreCase(status)) completed++;
            }
        }
        if (totalBookingsLabel != null) totalBookingsLabel.setText(String.valueOf(total));
        if (activeBookingsLabel != null) activeBookingsLabel.setText(String.valueOf(active));
        if (completedBookingsLabel != null) completedBookingsLabel.setText(String.valueOf(completed));
    }

    @FXML
    public void toggleEdit() {
        isEditing = !isEditing;
        if (nameField != null) nameField.setEditable(isEditing);
        if (phoneField != null) phoneField.setEditable(isEditing);
        if (editToggleBtn != null) editToggleBtn.setText(isEditing ? "✖ Cancel Edit" : "✏️ Edit");
        if (saveSection != null) {
            saveSection.setVisible(isEditing);
            saveSection.setManaged(isEditing);
        }
        if (saveStatusLabel != null) saveStatusLabel.setText("");
    }

    @FXML
    public void saveProfile() {
        String userId = LoginController.getUserId();
        String name = nameField != null ? nameField.getText().trim() : "";
        String phone = phoneField != null ? phoneField.getText().trim() : "";

        if (name.isBlank()) {
            if (saveStatusLabel != null) saveStatusLabel.setText("❌ Name cannot be empty.");
            return;
        }

        if (userId != null) {
            try {
                String json = String.format("{\"name\":\"%s\",\"phone\":\"%s\"}",
                        escapeJson(name), escapeJson(phone));
                String response = HttpUtil.sendPut(BASE_URL + "/user/" + userId, json);
                if (response != null && response.contains("\"id\"")) {
                    // Update session
                    LoginController.setUserName(name);
                    if (profileNameLabel != null) profileNameLabel.setText(name);
                    if (profileEmailLabel != null && emailField != null) {
                        profileEmailLabel.setText(emailField.getText());
                    }
                    if (avatarLabel != null && !name.isBlank()) {
                        avatarLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    }
                    if (saveStatusLabel != null) saveStatusLabel.setText("✅ Profile updated successfully.");
                    toggleEdit();
                } else {
                    if (saveStatusLabel != null)
                        saveStatusLabel.setText("❌ Failed to save profile. Please try again.");
                }
            } catch (Exception e) {
                if (saveStatusLabel != null)
                    saveStatusLabel.setText("❌ Error: " + e.getMessage());
            }
        } else {
            // Offline mode: update session only
            LoginController.setUserName(name);
            if (profileNameLabel != null) profileNameLabel.setText(name);
            if (avatarLabel != null && !name.isBlank()) {
                avatarLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());
            }
            if (saveStatusLabel != null) saveStatusLabel.setText("✅ Profile updated (offline mode).");
            toggleEdit();
        }
    }

    @FXML
    public void cancelEdit() {
        if (nameField != null) nameField.setText(LoginController.getUserName() != null ? LoginController.getUserName() : "");
        if (emailField != null) emailField.setText(LoginController.getUserEmail() != null ? LoginController.getUserEmail() : "");
        if (phoneField != null) phoneField.setText(LoginController.getUserPhone() != null ? LoginController.getUserPhone() : "");
        if (saveStatusLabel != null) saveStatusLabel.setText("");
        if (isEditing) toggleEdit();
    }

    @FXML
    public void goToLicense() {
        SceneNavigator.load("views/LicenseView.fxml");
    }

    // Navigation
    @FXML public void goToDashboard() { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML public void goToVehicles() { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML public void goToBookings() { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML public void goToProfile() { SceneNavigator.load("views/UserProfileView.fxml"); }
    @FXML public void goToPayment() { SceneNavigator.load("views/PaymentView.fxml"); }
    @FXML public void goToNotifications() { SceneNavigator.load("views/NotificationsView.fxml"); }
    @FXML public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
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

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "\u2026" : s;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
