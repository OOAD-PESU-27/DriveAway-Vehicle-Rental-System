package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.util.ArrayList;

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

    @FXML
    private VBox myBookingsContainer;

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
        
        // 👇 THE FIX: Force the date format to be exactly YYYY-MM-DD so the backend doesn't crash!
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedStart = start.format(formatter);
        String formattedEnd = end.format(formatter);

        String response = bookingService.createBooking(userId, vehicleId, formattedStart, formattedEnd);

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
    
    // ── NEW: Show My Bookings with handover button ────────────────────────────
    @FXML
    public void handleViewMyBookings() {
        if (myBookingsContainer == null) return;
        myBookingsContainer.getChildren().clear();
 
        String userId = LoginController.getUserId();
        String response = bookingService.getUserBookings(userId);
 
        if (response == null || response.equals("[]") || response.isEmpty()) {
            Label empty = new Label("No bookings found.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13;");
            myBookingsContainer.getChildren().add(empty);
            return;
        }
 
        List<String[]> bookings = parseBookings(response);
        for (String[] b : bookings) {
            myBookingsContainer.getChildren().add(buildCard(b));
        }
    }
 
    private VBox buildCard(String[] b) {
        // b = { id, vehicleId, status, totalPrice, startDate, endDate }
        String id     = b[0];
        String vid    = b[1];
        String status = b[2];
        String price  = b[3];
        String start  = b[4];
        String end    = b[5];
 
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: #f8fafc;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(30,64,175,0.08), 8, 0, 0, 2);"
        );
 
        // Row 1: Vehicle + status badge
        HBox row1 = new HBox();
        row1.setAlignment(Pos.CENTER_LEFT);
 
        Label vehicleLbl = new Label("🚗  " + vid);
        vehicleLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        vehicleLbl.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox.setHgrow(vehicleLbl, javafx.scene.layout.Priority.ALWAYS);
 
        Label badge = buildBadge(status);
        row1.getChildren().addAll(vehicleLbl, badge);
 
        // Row 2: Booking ID
        Label idLbl = new Label("ID: " + id);
        idLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");
 
        // Row 3: Dates + price
        HBox row3 = new HBox(16);
        Label dates = new Label("📅  " + start + "  →  " + end);
        dates.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");
        Label priceLbl = new Label("₹" + price);
        priceLbl.setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-font-size: 14;");
        row3.getChildren().addAll(dates, priceLbl);
 
        card.getChildren().addAll(row1, idLbl, row3);
 
        // ── CONFIRMED → Request Handover button ──────────────────────────────
        if ("CONFIRMED".equalsIgnoreCase(status)) {
            HBox actionRow = new HBox(10);
            actionRow.setAlignment(Pos.CENTER_LEFT);
 
            javafx.scene.control.Button btn = new javafx.scene.control.Button("🚀  Request Handover");
            btn.setStyle(
                "-fx-background-color: #7c3aed;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 7 18;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
            );
            Label result = new Label();
 
            btn.setOnAction(e -> {
                btn.setDisable(true);
                btn.setText("Processing...");
                String res = bookingService.handoverVehicle(id);
                if (res != null && res.contains("HANDED_OVER")) {
                    result.setText("✅  Handover requested! Staff will process it.");
                    result.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    btn.setVisible(false);
                    btn.setManaged(false);
                    // Refresh list after short delay
                    new Thread(() -> {
                        try { Thread.sleep(500); } catch (Exception ex) {}
                        javafx.application.Platform.runLater(this::handleViewMyBookings);
                    }).start();
                } else {
                    result.setText(res != null ? res : "Error — try again.");
                    result.setStyle("-fx-text-fill: #dc2626;");
                    btn.setDisable(false);
                    btn.setText("🚀  Request Handover");
                }
            });
 
            actionRow.getChildren().addAll(btn, result);
            card.getChildren().add(actionRow);
        }
 
        // ── HANDED_OVER → amber info label ───────────────────────────────────
        if ("HANDED_OVER".equalsIgnoreCase(status)) {
            Label info = new Label("⏳  Vehicle handed over — waiting for return inspection by staff.");
            info.setStyle("-fx-text-fill: #b45309; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(info);
        }
 
        // ── RETURNED → green success label ───────────────────────────────────
        if ("RETURNED".equalsIgnoreCase(status)) {
            Label info = new Label("✅  Trip completed. Thank you for choosing DriveAway!");
            info.setStyle("-fx-text-fill: #15803d; -fx-font-size: 12; -fx-font-weight: bold;");
            card.getChildren().add(info);
        }
 
        return card;
    }
 
    private Label buildBadge(String status) {
        Label badge = new Label(status);
        String bg;
        switch (status.toUpperCase()) {
            case "CONFIRMED":   bg = "#1d4ed8"; break;
            case "HANDED_OVER": bg = "#b45309"; break;
            case "RETURNED":    bg = "#15803d"; break;
            case "CANCELLED":   bg = "#b91c1c"; break;
            default:            bg = "#475569"; break;
        }
        badge.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 4 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;"
        );
        return badge;
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
    private List<String[]> parseBookings(String response) {

    List<String[]> bookings = new ArrayList<>();

    if (response == null || response.isEmpty()) {
        return bookings;
    }

    String cleaned = response
            .replace("[", "")
            .replace("]", "")
            .replace("{", "")
            .replace("}", "")
            .replace("\"", "");

    String[] tokens = cleaned.split(",");

    for (int i = 0; i + 5 < tokens.length; i += 6) {

        String id = tokens[i].split(":")[1];
        String vehicleId = tokens[i + 1].split(":")[1];
        String status = tokens[i + 2].split(":")[1];
        String price = tokens[i + 3].split(":")[1];
        String startDate = tokens[i + 4].split(":")[1];
        String endDate = tokens[i + 5].split(":")[1];

        bookings.add(new String[]{
                id,
                vehicleId,
                status,
                price,
                startDate,
                endDate
        });
    }

    return bookings;
}

    




    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}