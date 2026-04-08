package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookingController {

    private static final Logger logger = Logger.getLogger(BookingController.class.getName());

    @FXML
    private TextField vehicleIdField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label statusLabel;

    @FXML
    private CheckBox termsCheckBox;

    @FXML
    private Label termsAcceptedLabel;

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
        // Hide the accepted label initially
        if (termsAcceptedLabel != null) {
            termsAcceptedLabel.setVisible(false);
            termsAcceptedLabel.setManaged(false);
        }
    }

    /** Called when the T&C checkbox is toggled. */
    @FXML
    public void handleTermsCheckBox() {
        if (termsCheckBox == null || termsAcceptedLabel == null) return;
        boolean checked = termsCheckBox.isSelected();
        termsAcceptedLabel.setVisible(checked);
        termsAcceptedLabel.setManaged(checked);
    }

    /** Opens the Terms and Conditions page. */
    @FXML
    public void handleOpenTerms() {
        TermsAndConditionsController.setPreviousView("views/BookingView.fxml");
        SceneNavigator.load("views/TermsAndConditionsView.fxml");
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
        if (termsCheckBox != null && !termsCheckBox.isSelected()) {
            setStatus("Please accept the Terms and Conditions to proceed.");
            return;
        }

        setStatus("Processing booking...");
        String response = bookingService.createBooking(userId, vehicleId, start.toString(), end.toString());

        if (response != null && response.contains("\"id\"")) {
            String bookingId = extractField(response, "id");
            if (bookingId != null) {
                BookingManagementController.setLastBookingId(bookingId);
            }
            // Extract totalPrice and store for the payment page
            String totalPriceStr = extractNumericField(response, "totalPrice");
            if (totalPriceStr != null) {
                try {
                    BookingManagementController.setLastBookingTotalPrice(Double.parseDouble(totalPriceStr));
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Could not parse totalPrice ''{0}'' from booking response", totalPriceStr);
                }
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

    /** Extracts a numeric (non-quoted) JSON field value. */
    private String extractNumericField(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        if (start >= json.length()) return null;
        char ch = json.charAt(start);
        if (ch != '"') {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return end > start ? json.substring(start, end).trim() : null;
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

