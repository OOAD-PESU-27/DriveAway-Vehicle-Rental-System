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

        String response = vehicleService.getAllVehicles();
        if (response == null || response.isBlank() || response.equals("[]")) {
            response = vehicleService.getAvailableVehicles();
        }

        if (response == null || response.isBlank() || response.equals("[]")) {
            setStatus("No vehicles available. Make sure the backend is running.");
            resultCountLabel.setText("0 vehicles found");
            return;
        }

        List<String> jsonObjects = extractJsonObjects(response);
        for (String entry : jsonObjects) {
            String id = extract(entry, "id");
            String brand = extract(entry, "brand");
            String model = extract(entry, "model");
            String type = extract(entry, "vehicleType");
            String price = extract(entry, "pricePerDay");
            String fuel = extract(entry, "fuelType");
            String transmission = extract(entry, "transmission");
            String seats = extract(entry, "seatingCapacity");
            String available = extract(entry, "available");

            if (brand != null && model != null) {
                allVehicles.add(new String[]{
                        id, brand, model, type, price, fuel, transmission, seats, available
                });
            }
        }
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
        card.setMinWidth(235);
        card.setMaxWidth(250);

        // Card header with emoji
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(18, 12, 14, 12));
        header.setStyle("-fx-background-color: #f0f7ff; -fx-background-radius: 12 12 0 0;");

        Label icon = new Label(getVehicleEmoji(type));
        icon.setStyle("-fx-font-size: 52px;");

        boolean isAvailable = !"false".equalsIgnoreCase(avail);
        Label availLabel = new Label(isAvailable ? "✅ Available" : "❌ Unavailable");
        availLabel.getStyleClass().add(isAvailable ? "badge-active" : "badge-cancelled");
        header.getChildren().addAll(icon, availLabel);

        // Card body
        VBox body = new VBox(6);
        body.getStyleClass().add("vehicle-card-body");
        body.setPadding(new Insets(12, 14, 14, 14));

        Label nameLabel = new Label(brand + " " + model);
        nameLabel.getStyleClass().add("vehicle-name");
        nameLabel.setWrapText(true);

        Label typeLabel = new Label(type != null ? type : "Vehicle");
        typeLabel.getStyleClass().add("vehicle-type");

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
        if (start >= json.length()) return null;
        char ch = json.charAt(start);
        if (ch == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        } else if (ch == 'n') {
            return null;
        } else {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return end > start ? json.substring(start, end).trim() : null;
        }
    }
}
