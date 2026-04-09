package com.driveaway.controllers;

import com.driveaway.services.VehicleService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * VehicleCatalogController - Manages the vehicle catalog with grid display and filters
 */
public class VehicleCatalogController {

    @FXML private FlowPane vehicleGrid;
    @FXML private Label statusLabel;
    @FXML private Label resultCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> fuelFilter;
    @FXML private ComboBox<String> transmissionFilter;
    @FXML private ComboBox<String> availabilityFilter;

    private final VehicleService vehicleService = new VehicleService();
    private List<String[]> allVehicles = new ArrayList<>();

    @FXML
    public void initialize() {
        setupFilters();
        loadVehicles();
    }

    private void setupFilters() {
        typeFilter.getItems().addAll("All Types", "SEDAN", "SUV", "LUXURY", "ECONOMY", "TRUCK", "VAN");
        typeFilter.setValue("All Types");

        fuelFilter.getItems().addAll("All Fuel Types", "PETROL", "DIESEL", "ELECTRIC", "HYBRID");
        fuelFilter.setValue("All Fuel Types");

        transmissionFilter.getItems().addAll("Any", "AUTOMATIC", "MANUAL");
        transmissionFilter.setValue("Any");

        availabilityFilter.getItems().addAll("All", "Available", "Unavailable");
        availabilityFilter.setValue("All");
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
        availabilityFilter.setValue("All");
        if (searchField != null) searchField.clear();
        displayFilteredVehicles();
    }

    @FXML
    public void handleSearch() {
        displayFilteredVehicles();
    }

    private void loadVehicles() {
        setStatus("Loading vehicles...");
        vehicleGrid.getChildren().clear();
        allVehicles.clear();

        System.out.println("DEBUG: Fetching all vehicles from backend...");
        String response = vehicleService.getAllVehicles();
        System.out.println("DEBUG: getAllVehicles response: " + response);

        if (response == null || response.isBlank() || response.trim().equals("[]")) {
            System.out.println("DEBUG: getAllVehicles returned empty/null, trying getAvailableVehicles...");
            response = vehicleService.getAvailableVehicles();
            System.out.println("DEBUG: getAvailableVehicles response: " + response);
        }

        if (response == null || response.isBlank() || response.trim().equals("[]")) {
            System.out.println("DEBUG: Both endpoints returned no data. Backend may not be running.");
            setStatus("No vehicles available. Make sure the backend is running.");
            resultCountLabel.setText("0 vehicles found");
            return;
        }

        List<String> jsonObjects = extractJsonObjects(response);
        System.out.println("DEBUG: Parsed " + jsonObjects.size() + " vehicle objects from response");

        for (String entry : jsonObjects) {
            System.out.println("DEBUG: Processing vehicle entry: " + entry);
            String id = extract(entry, "id");
            String brand = extract(entry, "brand");
            String model = extract(entry, "model");
            String type = extract(entry, "vehicleType");
            String price = extract(entry, "pricePerDay");
            String fuel = extract(entry, "fuelType");
            String transmission = extract(entry, "transmission");
            String seats = extract(entry, "seatingCapacity");
            String available = extract(entry, "available");
            System.out.println("DEBUG: Extracted - id=" + id + ", brand=" + brand + ", model=" + model
                    + ", type=" + type + ", price=" + price + ", available=" + available);

            if (brand != null && model != null) {
                allVehicles.add(new String[]{
                        id, brand, model, type, price, fuel, transmission, seats, available
                });
            } else {
                System.out.println("DEBUG: Skipping entry - brand or model is null");
            }
        }
        System.out.println("DEBUG: Total vehicles loaded into list: " + allVehicles.size());
        setStatus("");
        displayFilteredVehicles();
    }

    /**
     * Extracts individual JSON objects from a JSON array string.
     * Uses brace-counting to correctly handle any JSON formatting,
     * including whitespace and multi-line responses.
     */
    private List<String> extractJsonObjects(String jsonArray) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++; // skip the next escaped character
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
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
        vehicleGrid.getChildren().clear();
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        String typeVal = typeFilter.getValue();
        String fuelVal = fuelFilter.getValue();
        String transVal = transmissionFilter.getValue();
        String availVal = availabilityFilter.getValue();

        List<String[]> filtered = new ArrayList<>();
        for (String[] v : allVehicles) {
            String brand = v[1] != null ? v[1] : "";
            String model = v[2] != null ? v[2] : "";
            String type = v[3] != null ? v[3] : "";
            String fuel = v[5] != null ? v[5] : "";
            String trans = v[6] != null ? v[6] : "";
            String avail = v[8] != null ? v[8] : "true";

            // Search filter
            if (!search.isEmpty() && !brand.toLowerCase().contains(search)
                    && !model.toLowerCase().contains(search)
                    && !type.toLowerCase().contains(search)) {
                continue;
            }
            // Type filter
            if (typeVal != null && !typeVal.equals("All Types")
                    && !type.equalsIgnoreCase(typeVal)) continue;
            // Fuel filter
            if (fuelVal != null && !fuelVal.equals("All Fuel Types")
                    && !fuel.equalsIgnoreCase(fuelVal)) continue;
            // Transmission filter
            if (transVal != null && !transVal.equals("Any")
                    && !trans.equalsIgnoreCase(transVal)) continue;
            // Availability filter
            if (availVal != null && availVal.equals("Available")
                    && !"true".equalsIgnoreCase(avail)) continue;
            if (availVal != null && availVal.equals("Unavailable")
                    && "true".equalsIgnoreCase(avail)) continue;

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
        card.setMinWidth(245);
        card.setMaxWidth(265);

        // ── Card photo/image header with type-specific gradient ──────────────
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(22, 12, 18, 12));
        header.setStyle(getCardHeaderStyle(type));

        // Large vehicle emoji
        Label icon = new Label(getVehicleEmoji(type));
        icon.setStyle("-fx-font-size: 56px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);");

        // Road / decorative stripe at bottom of header
        Label tagLine = new Label(getTypeTagLine(type));
        tagLine.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.80); "
                + "-fx-font-style: italic;");

        boolean isAvailable = !"false".equalsIgnoreCase(avail);
        Label availLabel = new Label(isAvailable ? "✅ Available" : "❌ Unavailable");
        availLabel.getStyleClass().add(isAvailable ? "badge-active" : "badge-cancelled");
        header.getChildren().addAll(icon, tagLine, availLabel);

        // ── Card body ────────────────────────────────────────────────────────
        VBox body = new VBox(6);
        body.getStyleClass().add("vehicle-card-body");
        body.setPadding(new Insets(12, 14, 14, 14));

        Label nameLabel = new Label(brand + " " + model);
        nameLabel.getStyleClass().add("vehicle-name");
        nameLabel.setWrapText(true);

        Label typeLabel = new Label(type != null ? type : "Vehicle");
        typeLabel.getStyleClass().add("vehicle-type");
        // Override vehicle-type badge colour to match card header
        typeLabel.setStyle(getTypeBadgeStyle(type));

        // Specs mini row
        HBox specs = new HBox(10);
        specs.setAlignment(Pos.CENTER_LEFT);
        if (fuel != null) {
            Label fuelLbl = new Label("⛽ " + fuel);
            fuelLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            specs.getChildren().add(fuelLbl);
        }
        if (seats != null) {
            Label seatsLbl = new Label("💺 " + seats);
            seatsLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            specs.getChildren().add(seatsLbl);
        }
        if (trans != null) {
            String transDisplay = switch (trans.toUpperCase()) {
                case "AUTOMATIC" -> "Auto";
                case "MANUAL" -> "Manual";
                default -> trans.length() > 6 ? trans.substring(0, 6) : trans;
            };
            Label transLbl = new Label("⚙ " + transDisplay);
            transLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            specs.getChildren().add(transLbl);
        }

        // Price
        HBox priceBox = new HBox(4);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label("₹" + (price != null ? price : "0"));
        priceLabel.getStyleClass().add("vehicle-price");
        Label perDay = new Label("/day");
        perDay.getStyleClass().add("vehicle-price-label");
        perDay.setStyle("-fx-font-size: 13px;");
        priceBox.getChildren().addAll(priceLabel, perDay);

        // Buttons
        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().addAll("btn-outline", "btn-small");
        detailsBtn.setOnAction(e -> openDetails(v));

        Button bookBtn = new Button("Book Now");
        bookBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        bookBtn.setDisable(!isAvailable);
        bookBtn.setOnAction(e -> bookVehicle(v));

        btnRow.getChildren().addAll(detailsBtn, bookBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        body.getChildren().addAll(nameLabel, typeLabel, specs, priceBox, spacer, btnRow);
        card.getChildren().addAll(header, body);
        return card;
    }

    /** Returns the inline style for the card photo header based on vehicle type. */
    private String getCardHeaderStyle(String type) {
        String base = "-fx-background-radius: 18 18 0 0; ";
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

    /** Returns a coloured badge style that matches the card header. */
    private String getTypeBadgeStyle(String type) {
        String base = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; "
                + "-fx-padding: 3 10 3 10; -fx-background-radius: 20; ";
        if (type == null) return base + "-fx-background-color: #6366f1;";
        return base + switch (type.toUpperCase()) {
            case "SEDAN"    -> "-fx-background-color: #1e40af;";
            case "SUV"      -> "-fx-background-color: #059669;";
            case "LUXURY"   -> "-fx-background-color: #d97706;";
            case "TRUCK"    -> "-fx-background-color: #dc2626;";
            case "VAN"      -> "-fx-background-color: #7c3aed;";
            case "ECONOMY"  -> "-fx-background-color: #0891b2;";
            case "BIKE", "MOTORCYCLE" -> "-fx-background-color: #78716c;";
            default -> "-fx-background-color: #6366f1;";
        };
    }

    /** Returns a short marketing tag line per vehicle type. */
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
        VehicleController.SelectedVehicleHolder.setSelectedVehicleId(v[0]);
        VehicleController.SelectedVehicleHolder.setSelectedVehicleData(v);
        SceneNavigator.load("views/VehicleDetailsView.fxml");
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

    private String extract(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        // Skip optional whitespace after colon
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        char ch = json.charAt(start);
        if (ch == '"') {
            // String value: find the closing quote, handling escaped quotes
            int end = start + 1;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '\\') {
                    end += 2; // skip both the backslash and the escaped character
                    continue;
                }
                if (c == '"') {
                    break;
                }
                end++;
            }
            return end < json.length() ? json.substring(start + 1, end) : null;
        } else if (ch == 'n') {
            return null; // null value
        } else {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return end > start ? json.substring(start, end).trim() : null;
        }
    }
}
