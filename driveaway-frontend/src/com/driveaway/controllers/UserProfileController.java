package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    @FXML private VBox saveSection;
    @FXML private Label saveStatusLabel;

    private final BookingService bookingService = new BookingService();
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        String userId = LoginController.getUserId();
        String userName = LoginController.getUserName();
        String userEmail = LoginController.getUserEmail();

        // Populate profile info
        if (userIdLabel != null) userIdLabel.setText("ID: " + (userId != null ? shorten(userId, 20) : "N/A"));
        if (profileNameLabel != null) profileNameLabel.setText(userName != null ? userName : "User");
        if (profileEmailLabel != null) profileEmailLabel.setText(userEmail != null ? userEmail : "");
        if (avatarLabel != null) {
            String initials = (userName != null && !userName.isBlank())
                    ? String.valueOf(userName.charAt(0)).toUpperCase() : "U";
            avatarLabel.setText(initials);
        }

        // Populate form fields
        if (nameField != null) nameField.setText(userName != null ? userName : "");
        if (emailField != null) emailField.setText(userEmail != null ? userEmail : "");
        if (phoneField != null) phoneField.setText(LoginController.getUserPhone() != null
                ? LoginController.getUserPhone() : "");

        if (licenseNumberField != null) licenseNumberField.setText("Verified ✓");
        if (licenseExpiryField != null) licenseExpiryField.setText("On file");

        // Load booking stats
        if (userId != null) loadBookingStats(userId);
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
            String status = extract(entry, "status");
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
        if (emailField != null) emailField.setEditable(isEditing);
        if (phoneField != null) phoneField.setEditable(isEditing);
        if (editToggleBtn != null) editToggleBtn.setText(isEditing ? "✖ Cancel Edit" : "✏️ Edit");
        if (saveSection != null) {
            saveSection.setVisible(isEditing);
            saveSection.setManaged(isEditing);
        }
    }

    @FXML
    public void saveProfile() {
        // In a real app, this would call the API to update user info
        String name = nameField != null ? nameField.getText() : "";
        String email = emailField != null ? emailField.getText() : "";

        if (name.isBlank()) {
            if (saveStatusLabel != null) saveStatusLabel.setText("Name cannot be empty.");
            return;
        }

        // Update display
        if (profileNameLabel != null) profileNameLabel.setText(name);
        if (profileEmailLabel != null) profileEmailLabel.setText(email);
        if (avatarLabel != null && !name.isBlank()) {
            avatarLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        // Update session data
        LoginController.setUserName(name);

        if (saveStatusLabel != null) saveStatusLabel.setText("✅ Profile updated successfully.");
        toggleEdit();
    }

    @FXML
    public void cancelEdit() {
        // Restore original values
        if (nameField != null) nameField.setText(LoginController.getUserName() != null ? LoginController.getUserName() : "");
        if (emailField != null) emailField.setText(LoginController.getUserEmail() != null ? LoginController.getUserEmail() : "");
        if (phoneField != null) phoneField.setText(LoginController.getUserPhone() != null ? LoginController.getUserPhone() : "");
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
    @FXML public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }

    private String extract(String json, String field) {
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
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
