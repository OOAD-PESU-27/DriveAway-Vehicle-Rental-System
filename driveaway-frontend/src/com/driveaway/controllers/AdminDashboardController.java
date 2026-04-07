package com.driveaway.controllers;

import com.driveaway.utils.HttpUtil;
import com.driveaway.utils.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;

/**
 * AdminDashboardController - Admin fleet management and report overview.
 * Requires user to be logged in (uses their ID as admin ID for local testing).
 */
public class AdminDashboardController {

    @FXML private Label adminIdLabel;
    @FXML private Label fleetCountLabel;
    @FXML private Label reportsCountLabel;
    @FXML private TableView<String[]> fleetTable;
    @FXML private TableColumn<String[], String> brandCol;
    @FXML private TableColumn<String[], String> modelCol;
    @FXML private TableColumn<String[], String> typeCol;
    @FXML private TableColumn<String[], String> priceCol;
    @FXML private TableColumn<String[], String> statusCol;
    @FXML private Label statusLabel;

    private static final String BASE_URL = "http://localhost:8080";

    private String adminId;

    @FXML
    public void initialize() {
        adminId = LoginController.getUserId();
        if (adminId == null) adminId = "ADMIN";
        if (adminIdLabel != null) adminIdLabel.setText("Admin ID: " + adminId);
        setupTable();
        loadFleet();
    }

    private void setupTable() {
        if (brandCol != null) brandCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 0 ? d.getValue()[0] : ""));
        if (modelCol != null) modelCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 1 ? d.getValue()[1] : ""));
        if (typeCol != null) typeCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 2 ? d.getValue()[2] : ""));
        if (priceCol != null) priceCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 3 ? "\u20b9" + d.getValue()[3] : ""));
        if (statusCol != null) statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 4 ? d.getValue()[4] : ""));
    }

    private void loadFleet() {
        String response = HttpUtil.sendGetWithHeader(
                BASE_URL + "/api/v1/admin/fleet", "X-Admin-ID", adminId);
        ObservableList<String[]> rows = FXCollections.observableArrayList();

        if (response == null || response.isBlank() || response.equals("[]")) {
            // Fall back to public vehicles endpoint
            response = HttpUtil.sendGet(BASE_URL + "/api/v1/vehicles");
        }

        if (response != null && !response.isBlank() && !response.equals("[]")) {
            String[] entries = response.replace("[{", "").replace("}]", "").split("\\},\\{");
            for (String entry : entries) {
                if (entry.isBlank()) continue;
                String brand = extractField(entry, "brand");
                String model = extractField(entry, "model");
                String vType = extractField(entry, "vehicleType");
                String price = extractField(entry, "pricePerDay");
                String status = extractField(entry, "status");
                rows.add(new String[]{
                        brand != null ? brand : "",
                        model != null ? model : "",
                        vType != null ? vType : "",
                        price != null ? price : "",
                        status != null ? status : "AVAILABLE"
                });
            }
        }

        if (fleetTable != null) fleetTable.setItems(rows);
        if (fleetCountLabel != null) fleetCountLabel.setText(String.valueOf(rows.size()));
        setStatus(rows.size() + " vehicle(s) in fleet.");
    }

    @FXML
    public void handleRefreshFleet() {
        loadFleet();
    }

    @FXML
    public void goToReports() {
        SceneNavigator.load("views/ReportsView.fxml");
    }

    @FXML
    public void goToNotifications() {
        SceneNavigator.load("views/NotificationsView.fxml");
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
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        return end > start ? json.substring(start, end).trim() : null;
    }
}
