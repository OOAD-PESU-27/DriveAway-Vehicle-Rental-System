package com.driveaway.controllers;

import com.driveaway.services.VehicleService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;

public class VehicleController {

    @FXML
    private ListView<String> vehicleListView;

    @FXML
    private Label statusLabel;

    private VehicleService vehicleService = new VehicleService();

    @FXML
    public void initialize() {
        loadVehicles();
    }

    @FXML
    public void handleRefresh() {
        loadVehicles();
    }

    @FXML
    public void handleBook() {
        String selected = vehicleListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Please select a vehicle first.");
            return;
        }
        // Pass selected vehicle info to booking view
        SelectedVehicleHolder.setSelectedVehicleInfo(selected);
        SceneNavigator.load("views/VehicleDetailsView.fxml");
    }

    @FXML
    public void goToDashboard() { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML
    public void goToCatalog() { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML
    public void goToBookings() { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML
    public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }

    private void loadVehicles() {
        setStatus("Loading vehicles...");
        vehicleListView.getItems().clear();

        String response = vehicleService.getAvailableVehicles();
        if (response == null || response.isBlank()) {
            setStatus("No vehicles available.");
            return;
        }

        // Parse simple JSON array of vehicle objects
        String[] entries = response.replace("[", "").replace("]", "").split("\\},\\{");
        for (String entry : entries) {
            String brand = extractField(entry, "brand");
            String model = extractField(entry, "model");
            String type = extractField(entry, "vehicleType");
            String price = extractField(entry, "pricePerDay");
            String id = extractField(entry, "id");
            if (brand != null && model != null) {
                vehicleListView.getItems().add(
                        id + " | " + brand + " " + model + " (" + type + ") - ₹" + price + "/day");
            }
        }
        setStatus("Found " + vehicleListView.getItems().size() + " vehicle(s).");
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        } else {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return json.substring(start, end).trim();
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * Simple holder to pass selected vehicle info between controllers
     */
    public static class SelectedVehicleHolder {
        private static String selectedVehicleInfo;
        private static String selectedVehicleId;
        private static String[] selectedVehicleData;

        public static void setSelectedVehicleInfo(String info) {
            selectedVehicleInfo = info;
            if (info != null && info.contains("|")) {
                selectedVehicleId = info.split("\\|")[0].trim();
            }
        }

        public static void setSelectedVehicleId(String id) {
            selectedVehicleId = id;
        }

        public static void setSelectedVehicleData(String[] data) {
            selectedVehicleData = data;
            if (data != null && data.length > 0) {
                selectedVehicleId = data[0];
            }
        }

        public static String getSelectedVehicleId() { return selectedVehicleId; }
        public static String getSelectedVehicleInfo() { return selectedVehicleInfo; }
        public static String[] getSelectedVehicleData() { return selectedVehicleData; }
    }
}