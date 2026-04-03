package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * VehicleDetailsController - Shows full vehicle details and booking form
 */
public class VehicleDetailsController {

    @FXML private Label vehicleNameLabel;
    @FXML private Label vehicleEmojiLabel;
    @FXML private Label vehicleTypeLabel;
    @FXML private Label availabilityLabel;
    @FXML private Label priceLabel;
    @FXML private Label brandLabel;
    @FXML private Label modelLabel;
    @FXML private Label yearLabel;
    @FXML private Label fuelLabel;
    @FXML private Label transmissionLabel;
    @FXML private Label seatsLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> locationCombo;
    @FXML private TextArea specialRequestsArea;
    @FXML private Label durationLabel;
    @FXML private Label rateLabel;
    @FXML private Label totalLabel;
    @FXML private CheckBox termsCheckBox;
    @FXML private Label statusLabel;

    private final BookingService bookingService = new BookingService();
    private String[] vehicleData;
    private double pricePerDay = 0;

    @FXML
    public void initialize() {
        vehicleData = VehicleController.SelectedVehicleHolder.getSelectedVehicleData();
        setupLocationCombo();
        setupDateDefaults();
        populateVehicleDetails();
    }

    private void setupLocationCombo() {
        if (locationCombo != null) {
            locationCombo.getItems().addAll(
                    "Mumbai Airport", "Delhi Airport", "Bangalore Airport",
                    "Chennai Airport", "Hyderabad Airport", "Pune City",
                    "Kolkata City", "Ahmedabad City"
            );
            locationCombo.setValue("Mumbai Airport");
        }
    }

    private void setupDateDefaults() {
        if (startDatePicker != null) {
            startDatePicker.setValue(LocalDate.now());
        }
        if (endDatePicker != null) {
            endDatePicker.setValue(LocalDate.now().plusDays(1));
        }
        calculateTotal();
    }

    private void populateVehicleDetails() {
        if (vehicleData == null) {
            // Fallback: try from the info string
            String info = VehicleController.SelectedVehicleHolder.getSelectedVehicleInfo();
            if (info != null && vehicleNameLabel != null) {
                vehicleNameLabel.setText(info);
            }
            return;
        }

        // vehicleData: [id, brand, model, type, price, fuel, transmission, seats, available]
        String brand = vehicleData.length > 1 ? vehicleData[1] : "";
        String model = vehicleData.length > 2 ? vehicleData[2] : "";
        String type = vehicleData.length > 3 ? vehicleData[3] : "";
        String price = vehicleData.length > 4 ? vehicleData[4] : "0";
        String fuel = vehicleData.length > 5 ? vehicleData[5] : "-";
        String trans = vehicleData.length > 6 ? vehicleData[6] : "-";
        String seats = vehicleData.length > 7 ? vehicleData[7] : "-";
        String avail = vehicleData.length > 8 ? vehicleData[8] : "true";

        if (vehicleNameLabel != null) vehicleNameLabel.setText(brand + " " + model);
        if (vehicleEmojiLabel != null) vehicleEmojiLabel.setText(getVehicleEmoji(type));
        if (vehicleTypeLabel != null) vehicleTypeLabel.setText(type != null ? type : "Vehicle");
        if (brandLabel != null) brandLabel.setText(brand != null ? brand : "-");
        if (modelLabel != null) modelLabel.setText(model != null ? model : "-");
        if (yearLabel != null) yearLabel.setText(String.valueOf(java.time.LocalDate.now().getYear()));
        if (fuelLabel != null) fuelLabel.setText(fuel != null ? fuel : "-");
        if (transmissionLabel != null) transmissionLabel.setText(trans != null ? trans : "-");
        if (seatsLabel != null) seatsLabel.setText(seats != null ? seats + " seats" : "-");

        try { pricePerDay = Double.parseDouble(price); } catch (Exception e) { pricePerDay = 0; }
        if (priceLabel != null) priceLabel.setText("₹" + String.format("%.0f", pricePerDay));
        if (rateLabel != null) rateLabel.setText("₹" + String.format("%.0f", pricePerDay));

        boolean isAvailable = !"false".equalsIgnoreCase(avail);
        if (availabilityLabel != null) {
            availabilityLabel.setText(isAvailable ? "✅ Available" : "❌ Unavailable");
            availabilityLabel.getStyleClass().setAll(isAvailable ? "badge-active" : "badge-cancelled");
        }

        calculateTotal();
    }

    @FXML
    public void calculateTotal() {
        if (startDatePicker == null || endDatePicker == null) return;
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null || end == null) return;

        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) {
            if (durationLabel != null) durationLabel.setText("Invalid dates");
            if (totalLabel != null) totalLabel.setText("₹0");
            return;
        }

        double total = days * pricePerDay;
        if (durationLabel != null) durationLabel.setText(days + " day" + (days != 1 ? "s" : ""));
        if (totalLabel != null) totalLabel.setText("₹" + String.format("%.0f", total));
    }

    @FXML
    public void handleBook() {
        String userId = LoginController.getUserId();
        if (userId == null) {
            setStatus("Please log in to book a vehicle.");
            return;
        }

        if (termsCheckBox != null && !termsCheckBox.isSelected()) {
            setStatus("Please accept the terms and conditions.");
            return;
        }

        LocalDate start = startDatePicker != null ? startDatePicker.getValue() : null;
        LocalDate end = endDatePicker != null ? endDatePicker.getValue() : null;

        if (start == null || end == null) {
            setStatus("Please select pick-up and drop-off dates.");
            return;
        }
        if (!end.isAfter(start)) {
            setStatus("Drop-off date must be after pick-up date.");
            return;
        }

        String vehicleId = vehicleData != null ? vehicleData[0] :
                VehicleController.SelectedVehicleHolder.getSelectedVehicleId();
        if (vehicleId == null) {
            setStatus("No vehicle selected.");
            return;
        }

        setStatus("Processing booking...");
        String response = bookingService.createBooking(userId, vehicleId,
                start.toString(), end.toString());

        if (response != null && (response.contains("\"id\"") || response.contains("id"))) {
            setStatus("✅ Booking confirmed! Redirecting to payment...");
            // Extract booking ID for payment
            String bookingId = extractField(response, "id");
            BookingManagementController.setLastBookingId(bookingId);
            SceneNavigator.load("views/PaymentView.fxml");
        } else {
            setStatus("❌ Booking failed. Please try again or check if backend is running.");
        }
    }

    private String getVehicleEmoji(String type) {
        if (type == null) return "🚗";
        return switch (type.toUpperCase()) {
            case "SUV" -> "🚙";
            case "LUXURY" -> "🏎";
            case "TRUCK" -> "🚛";
            case "VAN" -> "🚐";
            case "BIKE", "MOTORCYCLE" -> "🏍";
            default -> "🚗";
        };
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
