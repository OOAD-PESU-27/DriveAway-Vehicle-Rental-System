package com.driveaway.controllers;

import com.driveaway.utils.ApiClient;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

public class LicenseController {

    @FXML private TextField licenseField;
    @FXML private DatePicker expiryDatePicker;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        if (LoginController.getUserId() == null) {
            SceneNavigator.load("views/LoginView.fxml");
            return;
        }

        // 🔥 CALENDAR VISUALS: Only block dates in the past so the UI looks clean
        if (expiryDatePicker != null) {
            LocalDate today = LocalDate.now();
            
            final Callback<DatePicker, DateCell> dayCellFactory = new Callback<DatePicker, DateCell>() {
                @Override
                public DateCell call(final DatePicker datePicker) {
                    return new DateCell() {
                        @Override
                        public void updateItem(LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item.isBefore(today)) {
                                setDisable(true);
                                setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8;"); 
                            }
                        }
                    };
                }
            };
            expiryDatePicker.setDayCellFactory(dayCellFactory);
        }
    }

    @FXML
    public void handleSubmit() {
        String license = licenseField != null ? licenseField.getText().trim() : "";
        LocalDate expiryDate = expiryDatePicker != null ? expiryDatePicker.getValue() : null;

        if (license.isBlank()) { setStatus("License number is required."); return; }
        if (expiryDate == null) { setStatus("Please select an expiry date."); return; }

        String userId = LoginController.getUserId();
        if (userId == null) {
            setStatus("Session expired. Please login again.");
            SceneNavigator.load("views/LoginView.fxml");
            return;
        }

        // 🔥 THE 1-MONTH BUSINESS LOGIC (Previous strict rule)
        LocalDate today = LocalDate.now();
        LocalDate oneMonthFromToday = today.plusMonths(1);

        if (expiryDate.isBefore(oneMonthFromToday)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("License Validation Failed");
            alert.setHeaderText("License Expires Too Soon");
            alert.setContentText("Your license must be valid for at least one month from today (" + 
                                oneMonthFromToday.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + 
                                ") to rent a vehicle.");
            alert.showAndWait();
            return; 
        }

        setStatus("Verifying license...");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedExpiry = expiryDate.format(formatter);

        String jsonPayload = "{" +
                "\"userId\":\"" + userId + "\"," +
                "\"licenseNumber\":\"" + license + "\"," +
                "\"expiryDate\":\"" + formattedExpiry + "\"" +
                "}";

        try {
            System.out.println("Sending License to DB: " + jsonPayload);
            
            String response = com.driveaway.utils.ApiClient.post("/api/v1/licenses/create", jsonPayload);

            if (response == null || response.contains("\"status\":404") || response.contains("\"status\":500")) {
                response = com.driveaway.utils.ApiClient.post("/api/v1/license/create", jsonPayload);
            }
            if (response == null || response.contains("\"status\":404") || response.contains("\"status\":500")) {
                response = com.driveaway.utils.ApiClient.post("/licenses/create", jsonPayload);
            }
            if (response == null || response.contains("\"status\":404") || response.contains("\"status\":500")) {
                response = com.driveaway.utils.ApiClient.post("/license/add", jsonPayload);
            }

            if (response != null && !response.trim().isEmpty() && !response.contains("\"status\":404") && !response.contains("\"status\":400") && !response.contains("\"status\":500")) {
                
                System.out.println("License saved successfully!");
                
                Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
                prefs.putBoolean("has_license_" + userId, true);
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText("License Verified!");
                successAlert.setContentText("Your license has been successfully verified. You are ready to rent!");
                successAlert.showAndWait();

                SceneNavigator.load("views/DashboardView.fxml");
                
            } else {
                System.out.println("BACKEND REJECTED LICENSE: " + response);
                setStatus("Verification failed. Please try again.");
                
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Verification Failed");
                errorAlert.setHeaderText("We couldn't verify your license right now.");
                errorAlert.setContentText("Please try again later or check your network connection.");
                errorAlert.showAndWait();
            }
        } catch (Exception e) {
            setStatus("Connection Error. Is backend running?");
            e.printStackTrace();
        }
    } 

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }
}