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
        amountCol.setCellValueFactory(data -> {
            String amountStr = data.getValue().length > 5 ? data.getValue()[5] : "0";
            try {
                double amt = Double.parseDouble(amountStr);
                return new SimpleStringProperty("₹" + String.format("%,.0f", amt));
            } catch (NumberFormatException e) {
                return new SimpleStringProperty("₹" + amountStr);
            }
        });
    }

    private void loadDashboardData(String userId) {
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

            String paidAmountStr = extract(entry, "paidAmount");
            String totalPriceStr = extract(entry, "totalPrice");
            String amount = "0";
            if (paidAmountStr != null) {
                try {
                    double paid = Double.parseDouble(paidAmountStr);
                    if (paid > 0) amount = paidAmountStr;
                } catch (NumberFormatException ignored) {}
            }
            if ("0".equals(amount) && totalPriceStr != null && !totalPriceStr.isBlank()) {
                amount = totalPriceStr;
            }

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

    private VBox createMiniVehicleCard(String id, String brand, String model, String type, String price) {
        VBox card = new VBox(0);
        card.getStyleClass().add("vehicle-card");
        card.setMinWidth(195);
        card.setMaxWidth(195);

        VBox header = new VBox(5);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        header.setPadding(new Insets(16, 8, 14, 8));
        header.setStyle(getMiniCardHeaderStyle(type));

        String emoji = getVehicleEmoji(type);
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 40px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0, 0, 2);");

        Label typeLabel = new Label(type != null ? type.toUpperCase() : "VEHICLE");
        typeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.92); "
                + "-fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.18); "
                + "-fx-background-radius: 12; -fx-padding: 2 8 2 8;");
        header.getChildren().addAll(icon, typeLabel);

        VBox body = new VBox(6);
        body.setPadding(new Insets(10, 12, 12, 12));

        Label name = new Label(brand + " " + model);
        name.getStyleClass().add("vehicle-name");
        name.setWrapText(true);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        HBox priceBox = new HBox(4);
        Label priceLabel = new Label("₹" + (price != null ? price : "0"));
        priceLabel.getStyleClass().add("vehicle-price");
        priceLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        Label perDay = new Label("/day");
        perDay.getStyleClass().add("vehicle-price-label");
        priceBox.getChildren().addAll(priceLabel, perDay);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button bookBtn = new Button("Book Now 🚀");
        bookBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        String vehicleId = id;
        bookBtn.setOnAction(e -> {
            VehicleController.SelectedVehicleHolder.setSelectedVehicleId(vehicleId);
            VehicleController.SelectedVehicleHolder.setSelectedVehicleInfo(
                    vehicleId + " | " + brand + " " + model);
            SceneNavigator.load("views/VehicleDetailsView.fxml");
        });

        body.getChildren().addAll(name, priceBox, spacer, bookBtn);
        card.getChildren().addAll(header, body);
        return card;
    }

    private String getMiniCardHeaderStyle(String type) {
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
    @FXML public void goToPayment() { SceneNavigator.load("views/PaymentView.fxml"); }
    @FXML public void goToNotifications() { SceneNavigator.load("views/NotificationsView.fxml"); }
    @FXML public void goToReports() { SceneNavigator.load("views/ReportsView.fxml"); }
    @FXML public void goToAdminDashboard() { SceneNavigator.load("views/AdminDashboardView.fxml"); }
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