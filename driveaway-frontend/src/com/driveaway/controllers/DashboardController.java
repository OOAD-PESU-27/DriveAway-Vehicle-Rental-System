package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.services.VehicleService;
import com.driveaway.utils.SceneNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * DashboardController - Controls the main dashboard/home page
 * Shows stats, recent bookings, and featured vehicles
 */
public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label totalBookingsLabel;
    @FXML private Label activeBookingsLabel;
    @FXML private Label vehiclesLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label statusLabel;
    @FXML private Label vehiclesStatusLabel;
    @FXML private TableView<String[]> recentBookingsTable;
    @FXML private TableColumn<String[], String> bookingIdCol;
    @FXML private TableColumn<String[], String> vehicleCol;
    @FXML private TableColumn<String[], String> startDateCol;
    @FXML private TableColumn<String[], String> endDateCol;
    @FXML private TableColumn<String[], String> statusCol;
    @FXML private TableColumn<String[], String> amountCol;
    @FXML private HBox featuredVehiclesBox;

    private final BookingService bookingService = new BookingService();
    private final VehicleService vehicleService = new VehicleService();

    @FXML
    public void initialize() {
        String userId = LoginController.getUserId();
        String userName = LoginController.getUserName();

        if (userName != null && !userName.isBlank()) {
            userNameLabel.setText("Hello, " + userName + "!");
            welcomeLabel.setText("Welcome back, " + userName + "! Here's your rental overview.");
        } else if (userId != null) {
            userNameLabel.setText("Welcome!");
        }

        setupBookingsTable();
        loadDashboardData(userId);
    }

    private void setupBookingsTable() {
        bookingIdCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().length > 0 ? shorten(data.getValue()[0], 14) : ""));
        vehicleCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().length > 1 ? data.getValue()[1] : ""));
        startDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().length > 2 ? data.getValue()[2] : ""));
        endDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().length > 3 ? data.getValue()[3] : ""));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().length > 4 ? data.getValue()[4] : ""));
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().length > 5 ? "₹" + data.getValue()[5] : ""));
    }

    private void loadDashboardData(String userId) {
        // Load vehicles count
        String vehiclesJson = vehicleService.getAvailableVehicles();
        int vehicleCount = countEntries(vehiclesJson);
        vehiclesLabel.setText(String.valueOf(vehicleCount));

        if (vehicleCount > 0) {
            loadFeaturedVehicles(vehiclesJson);
        }

        if (userId == null) {
            setStatus("Please log in to see your bookings.");
            return;
        }

        // Load bookings
        String bookingsJson = bookingService.getUserBookings(userId);
        if (bookingsJson == null || bookingsJson.isBlank() || bookingsJson.equals("[]")) {
            totalBookingsLabel.setText("0");
            activeBookingsLabel.setText("0");
            totalSpentLabel.setText("₹0");
            return;
        }

        ObservableList<String[]> rows = FXCollections.observableArrayList();
        String[] entries = bookingsJson.replace("[", "").replace("]", "").split("\\},\\{");
        int total = 0, active = 0;
        double totalSpent = 0;

        for (String entry : entries) {
            String id = extract(entry, "id");
            String vehicleId = extract(entry, "vehicleId");
            String start = extract(entry, "startDate");
            String end = extract(entry, "endDate");
            String status = extract(entry, "status");
            String amount = extract(entry, "totalAmount");

            if (id != null) {
                rows.add(new String[]{id, vehicleId != null ? vehicleId : "-",
                        start != null ? start : "-", end != null ? end : "-",
                        status != null ? status : "PENDING",
                        amount != null ? amount : "0"});
                total++;
                if ("ACTIVE".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status)) {
                    active++;
                }
                if (amount != null) {
                    try { totalSpent += Double.parseDouble(amount); } catch (NumberFormatException ignored) {}
                }
            }
        }

        totalBookingsLabel.setText(String.valueOf(total));
        activeBookingsLabel.setText(String.valueOf(active));
        totalSpentLabel.setText("₹" + String.format("%.0f", totalSpent));

        // Show max 5 recent bookings
        ObservableList<String[]> recent = FXCollections.observableArrayList(
                rows.subList(0, Math.min(5, rows.size())));
        recentBookingsTable.setItems(recent);
    }

    private void loadFeaturedVehicles(String json) {
        featuredVehiclesBox.getChildren().clear();
        if (json == null || json.isBlank()) return;

        String[] entries = json.replace("[", "").replace("]", "").split("\\},\\{");
        int count = 0;
        for (String entry : entries) {
            if (count >= 4) break;
            String brand = extract(entry, "brand");
            String model = extract(entry, "model");
            String type = extract(entry, "vehicleType");
            String price = extract(entry, "pricePerDay");
            String id = extract(entry, "id");

            if (brand != null && model != null) {
                VBox card = createMiniVehicleCard(id, brand, model, type, price);
                featuredVehiclesBox.getChildren().add(card);
                count++;
            }
        }
        vehiclesStatusLabel.setText(count > 0 ? "" : "No vehicles available currently.");
    }

    private VBox createMiniVehicleCard(String id, String brand, String model,
                                       String type, String price) {
        VBox card = new VBox(8);
        card.getStyleClass().add("vehicle-card");
        card.setPadding(new Insets(14));
        card.setMinWidth(190);
        card.setMaxWidth(190);

        String emoji = getVehicleEmoji(type);
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 36px;");

        Label name = new Label(brand + " " + model);
        name.getStyleClass().add("vehicle-name");
        name.setWrapText(true);

        Label typeLabel = new Label(type != null ? type : "Vehicle");
        typeLabel.getStyleClass().add("vehicle-type");

        HBox priceBox = new HBox(4);
        Label priceLabel = new Label("₹" + (price != null ? price : "0"));
        priceLabel.getStyleClass().add("vehicle-price");
        Label perDay = new Label("/day");
        perDay.getStyleClass().add("vehicle-price-label");
        priceBox.getChildren().addAll(priceLabel, perDay);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button bookBtn = new Button("Book Now");
        bookBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        String vehicleId = id;
        bookBtn.setOnAction(e -> {
            VehicleController.SelectedVehicleHolder.setSelectedVehicleId(vehicleId);
            VehicleController.SelectedVehicleHolder.setSelectedVehicleInfo(
                    vehicleId + " | " + brand + " " + model);
            SceneNavigator.load("views/VehicleDetailsView.fxml");
        });

        card.getChildren().addAll(icon, name, typeLabel, priceBox, spacer, bookBtn);
        return card;
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

    // Navigation methods
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

    private int countEntries(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return 0;
        return json.split("\\},\\{").length;
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

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
