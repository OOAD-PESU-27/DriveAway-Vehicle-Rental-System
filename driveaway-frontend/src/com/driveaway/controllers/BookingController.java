package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;

public class BookingController {

    @FXML
    private TextField vehicleIdField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label statusLabel;

    private BookingService bookingService = new BookingService();

    @FXML
    public void initialize() {
        String vehicleId = VehicleController.SelectedVehicleHolder.getSelectedVehicleId();
        if (vehicleId != null && vehicleIdField != null) {
            vehicleIdField.setText(vehicleId);
        }
        if (startDatePicker != null) {
            startDatePicker.setValue(LocalDate.now());
        }
        if (endDatePicker != null) {
            endDatePicker.setValue(LocalDate.now().plusDays(1));
        }
    }

    @FXML
    public void handleBook() {
        String userId = LoginController.getUserId();
        if (userId == null) {
            setStatus("Please log in first.");
            return;
        }

        String vehicleId = vehicleIdField != null ? vehicleIdField.getText() : null;
        LocalDate start = startDatePicker != null ? startDatePicker.getValue() : null;
        LocalDate end = endDatePicker != null ? endDatePicker.getValue() : null;

        if (vehicleId == null || vehicleId.isBlank()) {
            setStatus("Vehicle ID is required.");
            return;
        }
        if (start == null || end == null) {
            setStatus("Please select start and end dates.");
            return;
        }
        if (!end.isAfter(start)) {
            setStatus("End date must be after start date.");
            return;
        }

        setStatus("Processing booking...");
        String response = bookingService.createBooking(userId, vehicleId, start.toString(), end.toString());

        if (response != null && response.contains("\"id\"")) {
            String bookingId = extractField(response, "id");
            if (bookingId != null) {
                BookingManagementController.setLastBookingId(bookingId);
            }
            setStatus("Booking confirmed!");
            SceneNavigator.load("views/PaymentView.fxml");
        } else {
            setStatus("Booking failed. Please try again.");
        }
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
        return null;
    }

    @FXML public void goToDashboard() { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML public void goToVehicles() { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML public void goToBookings() { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
