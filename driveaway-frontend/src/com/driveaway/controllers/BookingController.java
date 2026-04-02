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

        if (response != null && response.contains("id")) {
            setStatus("Booking confirmed!");
            SceneNavigator.load("views/PaymentView.fxml");
        } else {
            setStatus("Booking failed. Please try again.");
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
