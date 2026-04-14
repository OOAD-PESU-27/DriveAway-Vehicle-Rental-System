package com.driveaway.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.driveaway.services.BookingService;
import com.driveaway.services.VehicleService;
import com.driveaway.utils.SceneNavigator;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;



public class VehicleCatalogController {

    @FXML private FlowPane vehicleGrid;
    @FXML private Label statusLabel;
    @FXML private Label resultCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> fuelFilter;
    @FXML private ComboBox<String> transmissionFilter;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    private final VehicleService vehicleService = new VehicleService();
    private final BookingService bookingService = new BookingService();
    private static final Logger logger = Logger.getLogger(VehicleCatalogController.class.getName());
    private List<String[]> allVehicles = new ArrayList<>();

    @FXML
    public void handleCheckAvailability() {

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            setStatus("Please select start and end date");
            return;
        }

        String start = startDatePicker.getValue().toString();
        String end = endDatePicker.getValue().toString();

        System.out.println("📡 handleCheckAvailability called " + start + " to " + end);

        String response = vehicleService.getVehiclesWithPricing(start, end);

        if (response == null || response.isBlank()) {
            setStatus("Error fetching availability — no response from server");
            System.out.println("❌ Response is null or blank!");
            return;
        }

        if (!response.trim().startsWith("[") && !response.trim().startsWith("{")) {
            setStatus("Unexpected response format from server");
            System.out.println("❌ Response is not JSON: " + response);
            return;
        }

        List<String> jsonObjects = extractJsonObjects(response);
        System.out.println("📦 Parsed " + jsonObjects.size() + " vehicle objects");  // ← ADD THIS

        allVehicles.clear();

        for (String entry : jsonObjects) {

            System.out.println("🔍 Processing entry: " + entry);  // ← ADD THIS

            String id = extract(entry, "id");
            String name = extract(entry, "name");

            // ✅ NULL GUARD — this was the crash
            if (name == null || name.isBlank()) {
                System.out.println("⚠️ Skipping entry with null/blank name");
                continue;
            }

            String[] parts = name.split(" ", 2);
            String brand = parts[0];
            String model = parts.length > 1 ? parts[1] : "Vehicle";

            String price = extract(entry, "pricePerDay");
            String weekendPrice = extract(entry, "weekendPricePerDay");
            String holidayPrice = extract(entry, "holidayPricePerDay");
            String available = extract(entry, "available");
            String totalPrice = extract(entry, "totalPrice"); 
            String priceBreakdown = extract(entry, "priceBreakdown");

            System.out.println("   prices: base=" + price + " weekend=" + weekendPrice + " holiday=" + holidayPrice);
            String seats = extract(entry, "seatingCapacity");
            if (seats == null || seats.equals("0")) seats = "5";  // ← ADD THIS

            // Also extract vehicleType, fuelType, transmission if backend sends them
            String type = extract(entry, "vehicleType");
            if (type == null) type = "SUV";
            String fuel = extract(entry, "fuelType");
            if (fuel == null) fuel = "PETROL";
            String trans = extract(entry, "transmission");
            if (trans == null) trans = "AUTOMATIC";

            allVehicles.add(new String[]{
            id, brand, model, type, price, fuel, trans, seats,
            available != null ? available : "false",
            weekendPrice, holidayPrice, totalPrice , // ← v[11] = totalPrice
            priceBreakdown // ← v[12] = priceBreakdown
        });
        }

        System.out.println("Total vehicles loaded: " + allVehicles.size());  // ← ADD THIS
        // At end of handleCheckAvailability(), before displayFilteredVehicles():
        VehicleController.SelectedVehicleHolder.setSelectedStartDate(start);
        VehicleController.SelectedVehicleHolder.setSelectedEndDate(end);
        setStatus("");
        displayFilteredVehicles();
    }

   @FXML
    public void initialize() {
        System.out.println("🚀 VehicleCatalogController initialized");
        setupFilters();

        // ← RESTORE previously selected dates
        String savedStart = VehicleController.SelectedVehicleHolder.getSelectedStartDate();
        String savedEnd = VehicleController.SelectedVehicleHolder.getSelectedEndDate();

        if (savedStart != null && savedEnd != null) {
            startDatePicker.setValue(java.time.LocalDate.parse(savedStart));
            endDatePicker.setValue(java.time.LocalDate.parse(savedEnd));
            // Re-run availability check with saved dates
            handleCheckAvailability();
        } else {
            loadVehicles();
        }
    }

    private void setupFilters() {
        typeFilter.getItems().addAll("All Types", "SEDAN", "SUV", "LUXURY", "ECONOMY", "TRUCK", "VAN");
        typeFilter.setValue("All Types");

        fuelFilter.getItems().addAll("All Fuel Types", "PETROL", "DIESEL", "ELECTRIC", "HYBRID");
        fuelFilter.setValue("All Fuel Types");

        transmissionFilter.getItems().addAll("Any", "AUTOMATIC", "MANUAL");
        transmissionFilter.setValue("Any");
    }

    @FXML
    public void handleRefresh() {
        loadVehicles();
    }

    @FXML
    public void applyFilters() {
        displayFilteredVehicles();
    }

    @FXML
    public void clearFilters() {
        typeFilter.setValue("All Types");
        fuelFilter.setValue("All Fuel Types");
        transmissionFilter.setValue("Any");
        if (searchField != null) searchField.clear();
        displayFilteredVehicles();
    }

    @FXML
    public void handleSearch() {
        displayFilteredVehicles();
    }

    private void loadVehicles() {
        System.out.println("📡 Fetching all vehicles");
        setStatus("Loading vehicles...");
        vehicleGrid.getChildren().clear();
        allVehicles.clear();

        System.out.println("DEBUG: Fetching all vehicles from backend...");
        String response = vehicleService.getAllVehicles();

        if (response == null || response.isBlank() || response.trim().equals("[]") || response.contains("404")) {
            System.out.println("DEBUG: getAllVehicles failed, trying available vehicles...");
            response = vehicleService.getAvailableVehicles();
        }

        if (response == null || response.isBlank() || response.trim().equals("[]") || response.contains("404")) {
            setStatus("No vehicles available. Make sure the backend is running.");
            resultCountLabel.setText("0 vehicles found");
            return;
        }

        List<String> jsonObjects = extractJsonObjects(response);

        for (String entry : jsonObjects) {
            String id = extract(entry, "id");
            String brand = extract(entry, "brand");
            String model = extract(entry, "model");
            String name = extract(entry, "name");
            
            // 👉 THE SMART BRIDGE: If brand/model are missing, split the old "name" field!
            if ((brand == null || model == null) && name != null && !name.isEmpty()) {
                String[] parts = name.split(" ", 2);
                brand = parts[0];
                model = parts.length > 1 ? parts[1] : "Vehicle";
            }

            String type = extract(entry, "vehicleType");
            if (type == null) type = "SUV"; 

            String price = extract(entry, "pricePerDay");

            String fuel = extract(entry, "fuelType");
            if (fuel == null) fuel = "PETROL"; 

            String transmission = extract(entry, "transmission");
            if (transmission == null) transmission = "AUTOMATIC"; 

            String seats = extract(entry, "seatingCapacity");
            if (seats == null || seats.equals("0")) seats = "5"; 

            String available = "unknown";   // 👈 IMPORTANT

            if (brand != null && model != null) {
                allVehicles.add(new String[]{
                        id, brand, model, type, price, fuel, transmission, seats, available,null, null,null,null
                });
            }
        }
        
        setStatus("");
        displayFilteredVehicles();
    }

    private List<String> extractJsonObjects(String jsonArray) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (inString) {
                if (c == '\\') i++; 
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(jsonArray.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private void displayFilteredVehicles() {
        System.out.println("Applying filters to " + allVehicles.size() + " vehicles");
        vehicleGrid.getChildren().clear();
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        String typeVal = typeFilter.getValue();
        String fuelVal = fuelFilter.getValue();
        String transVal = transmissionFilter.getValue();
       

        List<String[]> filtered = new ArrayList<>();
        for (String[] v : allVehicles) {
            String brand = v[1] != null ? v[1] : "";
            String model = v[2] != null ? v[2] : "";
            String type = v[3] != null ? v[3] : "";
            String fuel = v[5] != null ? v[5] : "";
            String trans = v[6] != null ? v[6] : "";
            
            boolean isDateSelected = startDatePicker.getValue() != null && endDatePicker.getValue() != null;

            if (!search.isEmpty() && !brand.toLowerCase().contains(search)
                    && !model.toLowerCase().contains(search)
                    && !type.toLowerCase().contains(search)) continue;
            if (typeVal != null && !typeVal.equals("All Types") && !type.equalsIgnoreCase(typeVal)) continue;
            if (fuelVal != null && !fuelVal.equals("All Fuel Types") && !fuel.equalsIgnoreCase(fuelVal)) continue;
            if (transVal != null && !transVal.equals("Any") && !trans.equalsIgnoreCase(transVal)) continue;
           

            filtered.add(v);
        }

        resultCountLabel.setText(filtered.size() + " vehicle" + (filtered.size() != 1 ? "s" : "") + " found");

        for (String[] vehicle : filtered) {
            vehicleGrid.getChildren().add(createVehicleCard(vehicle));
        }

        if (filtered.isEmpty()) {
            Label empty = new Label("No vehicles match your filters.\nTry adjusting the search or filters.");
            empty.getStyleClass().add("text-muted");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-size: 15px; -fx-padding: 40;");
            vehicleGrid.getChildren().add(empty);
        }
    }

    private VBox createVehicleCard(String[] v) {
        String id = v[0];
        String brand = v[1];
        String model = v[2];
        String type = v[3];
        String price = v[4];
        String fuel = v[5];
        String trans = v[6];
        String seats = v[7];
        String avail = v[8];

        
        VBox card = new VBox(0);
        card.getStyleClass().add("vehicle-card");
        card.setMinWidth(248);
        card.setMaxWidth(270);
        card.setMinHeight(380);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        VBox header = new VBox(7);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24, 12, 20, 12));
        header.setStyle(getCardHeaderStyle(type));

        Label icon = new Label(getVehicleEmoji(type));
        icon.setStyle("-fx-font-size: 62px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 8, 0, 0, 3);");

        Label tagLine = new Label(getTypeTagLine(type));
        tagLine.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.88); -fx-font-style: italic; -fx-font-weight: bold;");

       boolean isDateSelected = startDatePicker.getValue() != null && endDatePicker.getValue() != null;

        Label availLabel;

        if (!isDateSelected || "unknown".equals(avail)) {
            availLabel = new Label("");   // hide initially
        } else {
            boolean isAvailableUI = "true".equalsIgnoreCase(avail);
            availLabel = new Label(isAvailableUI ? "✅ Available" : "❌ Unavailable");

            availLabel.setStyle(isAvailableUI
                ? "-fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 10;"
                : "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 10;");
        }

        header.getChildren().addAll(icon, tagLine, availLabel);

        VBox body = new VBox(7);
        body.getStyleClass().add("vehicle-card-body");
        body.setPadding(new Insets(13, 15, 15, 15));

        Label nameLabel = new Label(brand + " " + model);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");
        nameLabel.setWrapText(true);

        Label typeLabel = new Label(type != null ? type.toUpperCase() : "VEHICLE");
        typeLabel.setStyle(getTypeBadgeStyle(type));

        HBox specs = new HBox(8);
        specs.setAlignment(Pos.CENTER_LEFT);
        specs.setStyle("-fx-background-color: #f0f7ff; -fx-background-radius: 8; -fx-padding: 6 10 6 10; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-border-width: 1;");
        
        Label fuelLbl = new Label("⛽ " + fuel);
        fuelLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;");
        
        Label seatsLbl = new Label("💺 " + seats);
        seatsLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #059669; -fx-font-weight: bold;");
        
        String transDisplay = switch (trans.toUpperCase()) {
            case "AUTOMATIC" -> "Auto";
            case "MANUAL" -> "Manual";
            default -> trans.length() > 6 ? trans.substring(0, 6) : trans;
        };
        Label transLbl = new Label("⚙ " + transDisplay);
        transLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #7c3aed; -fx-font-weight: bold;");
        
        specs.getChildren().addAll(fuelLbl, seatsLbl, transLbl);

        // REPLACE everything from "String basePrice = v[4];" to "body.getChildren().addAll(...)"
        // WITH this:

        String basePrice = v[4];
        String weekendPrice = (v.length > 9 && v[9] != null) ? v[9] : null;
        String holidayPrice = (v.length > 10 && v[10] != null) ? v[10] : null;
        String totalPrice = (v.length > 11 && v[11] != null) ? v[11] : null;

        VBox priceBox = new VBox(4);
        priceBox.setPadding(new Insets(8, 0, 8, 0));
        priceBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 8;");

        if (basePrice != null) {
            Label base = new Label("💰 Base: ₹" + basePrice + "/day");
            base.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");
            priceBox.getChildren().add(base);
        }

        if (weekendPrice != null) {
            Label weekend = new Label("📅 Weekend: ₹" + weekendPrice + "/day");
            weekend.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
            priceBox.getChildren().add(weekend);
        }

        if (holidayPrice != null) {
            Label holiday = new Label("🎉 Holiday: ₹" + holidayPrice + "/day");
            holiday.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
            priceBox.getChildren().add(holiday);
        }

        if (totalPrice != null) {
        Label total = new Label("💳 Total: ₹" + totalPrice);
        total.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2b6cb0;");
        priceBox.getChildren().add(total);
        }

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        Button detailsBtn = new Button("Details");
        detailsBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-cursor: hand;");
        detailsBtn.setOnAction(e -> openDetails(v));

        Button bookBtn = new Button("Book Now 🚀");
        bookBtn.setStyle("-fx-background-color: #2b6cb0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        boolean isAvailable = isDateSelected && "true".equalsIgnoreCase(avail);
        bookBtn.setDisable(!isAvailable);
        bookBtn.setOnAction(e -> bookVehicle(v));

        btnRow.getChildren().addAll(detailsBtn, bookBtn);

        // NO spacer — just stack naturally
        body.getChildren().addAll(nameLabel, typeLabel, specs, priceBox, btnRow);
        card.getChildren().addAll(header, body);
        return card;
    }

    private String getCardHeaderStyle(String type) {
        String base = "-fx-background-radius: 15 15 0 0; ";
        if (type == null) return base + "-fx-background-color: linear-gradient(to bottom right, #312e81, #4f46e5, #818cf8);";
        return base + switch (type.toUpperCase()) {
            case "SEDAN"    -> "-fx-background-color: linear-gradient(to bottom right, #1e3a8a, #2563eb, #60a5fa);";
            case "SUV"      -> "-fx-background-color: linear-gradient(to bottom right, #064e3b, #059669, #34d399);";
            case "LUXURY"   -> "-fx-background-color: linear-gradient(to bottom right, #78350f, #b45309, #fcd34d);";
            case "TRUCK"    -> "-fx-background-color: linear-gradient(to bottom right, #7f1d1d, #dc2626, #f87171);";
            case "VAN"      -> "-fx-background-color: linear-gradient(to bottom right, #4c1d95, #6d28d9, #a78bfa);";
            case "ECONOMY"  -> "-fx-background-color: linear-gradient(to bottom right, #0c4a6e, #0284c7, #38bdf8);";
            case "BIKE", "MOTORCYCLE" -> "-fx-background-color: linear-gradient(to bottom right, #27272a, #71717a, #d4d4d8);";
            default -> "-fx-background-color: linear-gradient(to bottom right, #312e81, #4f46e5, #818cf8);";
        };
    }

    private String getTypeBadgeStyle(String type) {
        String base = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 3 10 3 10; -fx-background-radius: 20; ";
        if (type == null) return base + "-fx-background-color: #4f46e5;";
        return base + switch (type.toUpperCase()) {
            case "SEDAN"    -> "-fx-background-color: #2563eb;";
            case "SUV"      -> "-fx-background-color: #059669;";
            case "LUXURY"   -> "-fx-background-color: #b45309;";
            case "TRUCK"    -> "-fx-background-color: #dc2626;";
            case "VAN"      -> "-fx-background-color: #6d28d9;";
            case "ECONOMY"  -> "-fx-background-color: #0284c7;";
            case "BIKE", "MOTORCYCLE" -> "-fx-background-color: #71717a;";
            default -> "-fx-background-color: #4f46e5;";
        };
    }

    private String getTypeTagLine(String type) {
        if (type == null) return "Premium Rental";
        return switch (type.toUpperCase()) {
            case "SEDAN"    -> "Comfortable City Ride";
            case "SUV"      -> "Adventure Ready";
            case "LUXURY"   -> "Premium Experience";
            case "TRUCK"    -> "Heavy-Duty Power";
            case "VAN"      -> "Family & Group Travel";
            case "ECONOMY"  -> "Budget Friendly";
            case "BIKE", "MOTORCYCLE" -> "Fast & Fun";
            default -> "Premium Rental";
        };
    }

    private void openDetails(String[] v) {
        VehicleController.SelectedVehicleHolder.setSelectedVehicleId(v[0]);
        VehicleController.SelectedVehicleHolder.setSelectedVehicleData(v);
        SceneNavigator.load("views/VehicleDetailsView.fxml");
    }

    private void bookVehicle(String[] v) {
        String userId = LoginController.getUserId();
        if (userId == null) {
            setStatus("Please log in to book a vehicle.");
            return;
        }

        String vehicleId = v[0];
        String start = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : null;
        String end   = endDatePicker.getValue()   != null ? endDatePicker.getValue().toString()   : null;

        if (start == null || end == null) {
            setStatus("Please select start and end dates before booking.");
            return;
        }
        if (!endDatePicker.getValue().isAfter(startDatePicker.getValue())) {
            setStatus("End date must be after start date.");
            return;
        }

        // Store vehicle context so PaymentView / PaymentController can reference it
        VehicleController.SelectedVehicleHolder.setSelectedVehicleId(vehicleId);
        VehicleController.SelectedVehicleHolder.setSelectedVehicleData(v);

        setStatus("⏳ Creating booking, please wait...");

        String response = bookingService.createBooking(userId, vehicleId, start, end);

        if (response != null && response.contains("\"id\"")) {
            // Store booking ID
            String bookingId = extractBookingField(response, "id");
            if (bookingId != null) {
                BookingManagementController.setLastBookingId(bookingId);
            }

            // Store totalPrice from booking response (preferred) or fall back to catalog price
            String totalPriceStr = extractNumericField(response, "totalPrice");
            if (totalPriceStr != null) {
                try {
                    BookingManagementController.setLastBookingTotalPrice(Double.parseDouble(totalPriceStr));
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Could not parse totalPrice ''{0}'' from booking response", totalPriceStr);
                }
            } else if (v.length > 11 && v[11] != null) {
                // Fall back to pre-computed catalog total (v[11] = totalPrice from pricing endpoint)
                try {
                    BookingManagementController.setLastBookingTotalPrice(Double.parseDouble(v[11]));
                } catch (NumberFormatException ignored) {}
            }

            setStatus("✅ Booking confirmed! Redirecting to payment...");
            SceneNavigator.load("views/PaymentView.fxml");
        } else {
            // FIX: Show the actual error message returned by the backend instead of a
            // generic message. This surfaces real reasons like "Vehicle is already booked
            // for the selected period" or "End date must be after start date".
            String errorDetail = "";
            if (response != null && !response.isBlank()) {
                // Strip JSON quotes/braces if the backend returned a plain string body
                errorDetail = response.trim()
                        .replaceAll("^\"", "").replaceAll("\"$", "")  // unwrap bare quoted string
                        .replaceAll("^\\{\\.\\*\\}$", response); // leave JSON objects as-is
                // Truncate very long responses for display
                if (errorDetail.length() > 200) errorDetail = errorDetail.substring(0, 200) + "...";
                errorDetail = ": " + errorDetail;
            }
            setStatus("❌ Booking failed" + errorDetail);
        }
    }

    /** Extracts a quoted string field from a JSON response. */
    private String extractBookingField(String json, String field) {
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

    /** Extracts a numeric (unquoted) field from a JSON response. */
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

private String extract(String json, String field) {
    String key = "\"" + field + "\":";
    int idx = json.indexOf(key);
    if (idx < 0) return null;

    int start = idx + key.length();

    while (start < json.length() && json.charAt(start) == ' ') start++;

    if (start >= json.length()) return null;

    char ch = json.charAt(start);

    // ✅ STRING VALUE (important for priceBreakdown)
    if (ch == '"') {
        StringBuilder sb = new StringBuilder();
        int i = start + 1;

        while (i < json.length()) {
            char c = json.charAt(i);

            if (c == '\\') {   // handle escape
                if (i + 1 < json.length()) {
                    sb.append(json.charAt(i + 1));
                    i += 2;
                    continue;
                }
            }

            if (c == '"') break;

            sb.append(c);
            i++;
        }

        return sb.toString();
    }

    // NULL case
    if (ch == 'n') return null;

    // NUMBER / BOOLEAN
    int end = json.indexOf(',', start);
    if (end < 0) end = json.indexOf('}', start);

    return end > start ? json.substring(start, end).trim() : null;
}

}