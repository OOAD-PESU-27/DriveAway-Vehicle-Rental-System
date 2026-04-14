package com.driveaway.controllers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.driveaway.services.BookingService;
import com.driveaway.utils.SceneNavigator;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * VehicleDetailsController - Shows full vehicle details and booking form
 */
public class VehicleDetailsController {

    @FXML private Label vehicleNameLabel;
    @FXML private VBox vehicleImageBox;
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
    @FXML private Label termsAcceptedLabel;
    @FXML private Label statusLabel;

    private final BookingService bookingService = new BookingService();
    private String[] vehicleData;
    private double pricePerDay = 0;

    @FXML
    public void initialize() {
        System.out.println("🔥 VehicleDetailsController initialize() called");
        vehicleData = VehicleController.SelectedVehicleHolder.getSelectedVehicleData();
        setupLocationCombo();
        setupDateDefaults();
        System.out.println("👉 Calling populateVehicleDetails()");
        populateVehicleDetails();
        // Hide the accepted label initially
        if (termsAcceptedLabel != null) {
            termsAcceptedLabel.setVisible(false);
            termsAcceptedLabel.setManaged(false);
        }
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
    String savedStart = VehicleController.SelectedVehicleHolder.getSelectedStartDate();
    String savedEnd = VehicleController.SelectedVehicleHolder.getSelectedEndDate();

    if (savedStart != null && startDatePicker != null) {
        startDatePicker.setValue(java.time.LocalDate.parse(savedStart));
        System.out.println("📅 Restored start date: " + savedStart);
    } else if (startDatePicker != null) {
        startDatePicker.setValue(LocalDate.now());
    }

    if (savedEnd != null && endDatePicker != null) {
        endDatePicker.setValue(java.time.LocalDate.parse(savedEnd));
        System.out.println("📅 Restored end date: " + savedEnd);
    } else if (endDatePicker != null) {
        endDatePicker.setValue(LocalDate.now().plusDays(1));
    }

    calculateTotal();
    }

  private void populateVehicleDetails() {



    vehicleData = VehicleController.SelectedVehicleHolder.getSelectedVehicleData();

    System.out.println("\n\n\n\n================ VEHICLE DETAILS DEBUG ================");

if (vehicleData == null) {
    System.out.println("❌ vehicleData is NULL");
    return;
}

System.out.println("📦 vehicleData length = " + vehicleData.length);

for (int i = 0; i < vehicleData.length; i++) {
    System.out.println("v[" + i + "] = " + vehicleData[i]);
}

System.out.println("=====================================================\n");
    if (vehicleData == null) {
        System.out.println("❌ vehicleData is NULL");
        return;
    }

    // 🔍 DEBUG: print full array
    System.out.println("📦 vehicleData length = " + vehicleData.length);
    for (int i = 0; i < vehicleData.length; i++) {
        System.out.println("   v[" + i + "] = " + vehicleData[i]);
    }

    // Extract fields
    String brand = vehicleData.length > 1 ? vehicleData[1] : "";
    String model = vehicleData.length > 2 ? vehicleData[2] : "";
    String type = vehicleData.length > 3 ? vehicleData[3] : "";
    String price = vehicleData.length > 4 ? vehicleData[4] : "0";
    String fuel = vehicleData.length > 5 ? vehicleData[5] : "-";
    String trans = vehicleData.length > 6 ? vehicleData[6] : "-";
    String seats = vehicleData.length > 7 ? vehicleData[7] : "-";
    String avail = vehicleData.length > 8 ? vehicleData[8] : "true";

    String weekendPrice = vehicleData.length > 9 ? vehicleData[9] : null;
    String holidayPrice = vehicleData.length > 10 ? vehicleData[10] : null;
    String totalPrice = vehicleData.length > 11 ? vehicleData[11] : null;
    String breakdown = vehicleData.length > 12 ? vehicleData[12] : null;

    // 🔍 DEBUG PRINTS (IMPORTANT)
    System.out.println("💰 Base Price = " + price);
    System.out.println("📅 Weekend Price = " + weekendPrice);
    System.out.println("🎉 Holiday Price = " + holidayPrice);
    System.out.println("💳 Total Price = " + totalPrice);
    System.out.println("📋 Breakdown Raw = " + breakdown);

    // Set basic UI
    if (vehicleNameLabel != null) vehicleNameLabel.setText(brand + " " + model);
    if (vehicleEmojiLabel != null) vehicleEmojiLabel.setText(getVehicleEmoji(type));
    if (vehicleTypeLabel != null) vehicleTypeLabel.setText(type);

    if (brandLabel != null) brandLabel.setText(brand);
    if (modelLabel != null) modelLabel.setText(model);
    if (yearLabel != null) yearLabel.setText(String.valueOf(java.time.LocalDate.now().getYear()));
    if (fuelLabel != null) fuelLabel.setText(fuel);
    if (transmissionLabel != null) transmissionLabel.setText(trans);
    if (seatsLabel != null) seatsLabel.setText(seats + " seats");

    // 🎨 Card styling
    if (vehicleImageBox != null) {
        vehicleImageBox.setStyle(getVehicleImageStyle(type));
    }

    // 💰 Base price display
    try {
        pricePerDay = Double.parseDouble(price);
    } catch (Exception e) {
        pricePerDay = 0;
    }

    if (priceLabel != null) {
        priceLabel.setText("₹" + String.format("%.0f", pricePerDay));
    }

    // 📋 BREAKDOWN (FROM BACKEND ONLY)
    if (breakdown != null && rateLabel != null) {

        String cleanBreakdown = breakdown
                .replace("? ?", "× ₹")
                .replace("?", "₹")
                .replace("\\n", "\n");

        System.out.println("✅ Clean Breakdown:\n" + cleanBreakdown);

        rateLabel.setText(cleanBreakdown);
        rateLabel.setWrapText(true);
    }

    // 💳 TOTAL (FROM BACKEND ONLY — NO CALCULATION)
    if (totalPrice != null && totalLabel != null) {
        System.out.println("✅ Using backend total: ₹" + totalPrice);
        totalLabel.setText("₹" + totalPrice);
    }

    // Availability
    boolean isAvailable = !"false".equalsIgnoreCase(avail);
    if (availabilityLabel != null) {
        availabilityLabel.setText(isAvailable ? "✅ Available" : "❌ Unavailable");
        availabilityLabel.getStyleClass().setAll(
                isAvailable ? "badge-active" : "badge-cancelled"
        );
    }

    // Duration (only display, NOT pricing)
    if (startDatePicker != null && endDatePicker != null) {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start != null && end != null) {
            long days = ChronoUnit.DAYS.between(start, end);
            if (durationLabel != null) {
                durationLabel.setText(days + " day" + (days != 1 ? "s" : ""));
            }
        }
    }
}
    /** Returns a vehicle-type-specific gradient background style string. */
    private String getVehicleImageStyle(String type) {
        if (type == null) return getDefaultImageStyle();
        String gradient = switch (type.toUpperCase()) {
            case "SEDAN"    -> "linear-gradient(to bottom right, #1e3a8a, #2563eb, #60a5fa)";
            case "SUV"      -> "linear-gradient(to bottom right, #064e3b, #059669, #34d399)";
            case "LUXURY"   -> "linear-gradient(to bottom right, #78350f, #b45309, #fcd34d)";
            case "TRUCK"    -> "linear-gradient(to bottom right, #7f1d1d, #dc2626, #f87171)";
            case "VAN"      -> "linear-gradient(to bottom right, #4c1d95, #6d28d9, #a78bfa)";
            case "ECONOMY"  -> "linear-gradient(to bottom right, #0c4a6e, #0284c7, #38bdf8)";
            case "BIKE", "MOTORCYCLE" -> "linear-gradient(to bottom right, #27272a, #71717a, #d4d4d8)";
            default         -> getDefaultImageStyle();
        };
        return "-fx-background-color: " + gradient + "; -fx-background-radius: 18; "
                + "-fx-effect: dropshadow(gaussian, rgba(30,64,175,0.22), 22, 0, 0, 7); "
                + "-fx-min-height: 210; -fx-min-width: 300; -fx-max-width: 320;";
    }

    private String getDefaultImageStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, #312e81, #4f46e5, #818cf8); "
                + "-fx-background-radius: 18; "
                + "-fx-effect: dropshadow(gaussian, rgba(30,64,175,0.22), 22, 0, 0, 7); "
                + "-fx-min-height: 210; -fx-min-width: 300; -fx-max-width: 320;";
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
        TermsAndConditionsController.setPreviousView("views/VehicleDetailsView.fxml");
        SceneNavigator.load("views/TermsAndConditionsView.fxml");
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
        LocalDate end   = endDatePicker   != null ? endDatePicker.getValue()   : null;

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

        setStatus("⏳ Processing booking...");
        String response = bookingService.createBooking(userId, vehicleId,
                start.toString(), end.toString());

        if (response != null && (response.contains("\"id\"") || response.contains("id"))) {
            // Store booking ID
            String bookingId = extractField(response, "id");
            BookingManagementController.setLastBookingId(bookingId);

            // Store totalPrice — try booking response first, then fall back to catalog/details price
            String totalPriceStr = extractNumericField(response, "totalPrice");
            if (totalPriceStr != null) {
                try {
                    BookingManagementController.setLastBookingTotalPrice(Double.parseDouble(totalPriceStr));
                } catch (NumberFormatException ignored) {}
            } else if (vehicleData != null && vehicleData.length > 11 && vehicleData[11] != null) {
                try {
                    BookingManagementController.setLastBookingTotalPrice(Double.parseDouble(vehicleData[11]));
                } catch (NumberFormatException ignored) {}
            } else if (pricePerDay > 0 && start != null && end != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
                if (days > 0) {
                    BookingManagementController.setLastBookingTotalPrice(days * pricePerDay);
                }
            }

            setStatus("✅ Booking confirmed! Redirecting to payment...");
            SceneNavigator.load("views/PaymentView.fxml");
        } else {
            setStatus("❌ Booking failed. Please try again or check if backend is running.");
        }
    }

    /** Extracts a numeric (unquoted) JSON field value. */
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

