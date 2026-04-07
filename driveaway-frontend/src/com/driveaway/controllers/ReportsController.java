package com.driveaway.controllers;

import com.driveaway.services.ReportService;
import com.driveaway.utils.HttpUtil;
import com.driveaway.utils.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;

/**
 * ReportsController - Displays and generates reports.
 * Provides vehicle inventory, daily, weekly, monthly payment reports.
 */
public class ReportsController {

    @FXML private TableView<String[]> reportsTable;
    @FXML private TableColumn<String[], String> typeCol;
    @FXML private TableColumn<String[], String> titleCol;
    @FXML private TableColumn<String[], String> totalTxnCol;
    @FXML private TableColumn<String[], String> revenueCol;
    @FXML private TableColumn<String[], String> generatedAtCol;
    @FXML private Label statusLabel;
    @FXML private Label vehicleStatsLabel;

    private static final String BASE_URL = "http://localhost:8080";

    private final ReportService reportService = new ReportService();
    private final String adminId;

    public ReportsController() {
        String uid = LoginController.getUserId();
        this.adminId = uid != null ? uid : "ADMIN";
    }

    @FXML
    public void initialize() {
        setupTable();
        loadReports();
    }

    private void setupTable() {
        if (typeCol != null) typeCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 0 ? d.getValue()[0] : ""));
        if (titleCol != null) titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 1 ? d.getValue()[1] : ""));
        if (totalTxnCol != null) totalTxnCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 2 ? d.getValue()[2] : ""));
        if (revenueCol != null) revenueCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 3 ? d.getValue()[3] : ""));
        if (generatedAtCol != null) generatedAtCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 4 ? shorten(d.getValue()[4], 19) : ""));
    }

    private void loadReports() {
        String response = reportService.getAllReports(adminId);
        ObservableList<String[]> rows = FXCollections.observableArrayList();

        if (response == null || response.isBlank() || response.equals("[]")) {
            setStatus("No reports yet. Generate one using the buttons above.");
            if (reportsTable != null) reportsTable.setItems(rows);
            return;
        }

        String[] entries = response.replace("[{", "").replace("}]", "").split("\\},\\{");
        for (String entry : entries) {
            if (entry.isBlank()) continue;
            String type = extractField(entry, "reportType");
            String title = extractField(entry, "title");
            String txn = extractField(entry, "totalTransactions");
            String rev = extractField(entry, "totalRevenue");
            String genAt = extractField(entry, "generatedAt");
            rows.add(new String[]{
                    type != null ? type : "",
                    title != null ? title : "",
                    txn != null ? txn : "0",
                    rev != null ? "\u20b9" + rev : "\u20b90",
                    genAt != null ? genAt : ""
            });
        }

        if (reportsTable != null) reportsTable.setItems(rows);
        setStatus(rows.size() + " report(s) loaded.");
    }

    @FXML
    public void handleGenerateVehicleReport() {
        setStatus("Generating vehicle inventory report...");
        String response = reportService.generateVehicleReport(adminId);
        if (response != null && response.contains("\"success\":true")) {
            String total = extractField(response, "totalVehicles");
            String available = extractField(response, "availableVehicles");
            String booked = extractField(response, "bookedVehicles");
            String maintenance = extractField(response, "maintenanceVehicles");
            if (vehicleStatsLabel != null) {
                vehicleStatsLabel.setText(String.format(
                        "Fleet: Total=%s | Available=%s | Booked=%s | Maintenance=%s",
                        total, available, booked, maintenance));
            }
            setStatus("✅ Vehicle inventory report generated.");
            loadReports();
        } else {
            setStatus("❌ Failed to generate vehicle report.");
        }
    }

    @FXML
    public void handleGenerateDailyReport() {
        setStatus("Generating daily report...");
        String response = reportService.generateDailyReport(adminId);
        if (response != null && response.contains("\"success\":true")) {
            setStatus("✅ Daily report generated.");
            loadReports();
        } else {
            setStatus("❌ Failed to generate daily report.");
        }
    }

    @FXML
    public void handleGenerateWeeklyReport() {
        setStatus("Generating weekly report...");
        String response = reportService.generateWeeklyReport(adminId);
        if (response != null && response.contains("\"success\":true")) {
            setStatus("✅ Weekly report generated.");
            loadReports();
        } else {
            setStatus("❌ Failed to generate weekly report.");
        }
    }

    @FXML
    public void handleGenerateMonthlyReport() {
        setStatus("Generating monthly report...");
        String response = reportService.generateMonthlyReport(adminId);
        if (response != null && response.contains("\"success\":true")) {
            setStatus("✅ Monthly report generated.");
            loadReports();
        } else {
            setStatus("❌ Failed to generate monthly report.");
        }
    }

    @FXML
    public void handleRefresh() {
        loadReports();
    }

    // Navigation
    @FXML public void goToDashboard() { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML public void goToVehicles() { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML public void goToBookings() { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML public void goToProfile() { SceneNavigator.load("views/UserProfileView.fxml"); }
    @FXML public void goToNotifications() { SceneNavigator.load("views/NotificationsView.fxml"); }
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
        // Number or boolean
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        return end > start ? json.substring(start, end).trim() : null;
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "\u2026" : s;
    }
}
