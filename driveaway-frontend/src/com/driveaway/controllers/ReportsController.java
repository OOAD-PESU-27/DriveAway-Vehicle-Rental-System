package com.driveaway.controllers;

import com.driveaway.services.ReportService;
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

    // Summary stat card labels
    @FXML private Label totalVehiclesLabel;
    @FXML private Label availableVehiclesLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalReportsLabel;

    private static final String BASE_URL = "http://localhost:8080";

    private final ReportService reportService = new ReportService();
    private final String adminId;

    /** Accumulated revenue from all loaded report rows (for the summary card). */
    private double accumulatedRevenue = 0.0;

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
        if (typeCol != null) {
            typeCol.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().length > 0 ? d.getValue()[0] : ""));
            // Colour-coded cell for report type
            typeCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        String style = getTypeStyle(item);
                        setStyle(style);
                    }
                }
            });
        }
        if (titleCol != null) titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 1 ? d.getValue()[1] : ""));
        if (totalTxnCol != null) {
            totalTxnCol.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().length > 2 ? d.getValue()[2] : ""));
            totalTxnCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #1e40af; -fx-alignment: CENTER;");
                    }
                }
            });
        }
        if (revenueCol != null) {
            revenueCol.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().length > 3 ? d.getValue()[3] : ""));
            revenueCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #15803d; -fx-font-size: 13px;");
                    }
                }
            });
        }
        if (generatedAtCol != null) generatedAtCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 4 ? shorten(d.getValue()[4], 19) : ""));
    }

    /** Returns an inline style for the report type cell based on the type value. */
    private String getTypeStyle(String type) {
        if (type == null) return "";
        return switch (type.toUpperCase()) {
            case "VEHICLE_INVENTORY", "VEHICLE" ->
                    "-fx-font-weight: bold; -fx-text-fill: white; "
                    + "-fx-background-color: #1e40af; -fx-background-radius: 6; -fx-padding: 3 8 3 8;";
            case "DAILY" ->
                    "-fx-font-weight: bold; -fx-text-fill: white; "
                    + "-fx-background-color: #059669; -fx-background-radius: 6; -fx-padding: 3 8 3 8;";
            case "WEEKLY" ->
                    "-fx-font-weight: bold; -fx-text-fill: white; "
                    + "-fx-background-color: #d97706; -fx-background-radius: 6; -fx-padding: 3 8 3 8;";
            case "MONTHLY" ->
                    "-fx-font-weight: bold; -fx-text-fill: white; "
                    + "-fx-background-color: #7c3aed; -fx-background-radius: 6; -fx-padding: 3 8 3 8;";
            default ->
                    "-fx-font-weight: bold; -fx-text-fill: #374151;";
        };
    }

    private void loadReports() {
        String response = reportService.getAllReports(adminId);
        ObservableList<String[]> rows = FXCollections.observableArrayList();
        accumulatedRevenue = 0.0;

        if (response == null || response.isBlank() || response.equals("[]")) {
            setStatus("No reports yet. Generate one using the cards above.");
            if (reportsTable != null) reportsTable.setItems(rows);
            updateSummaryCards(rows.size());
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

            // Accumulate revenue for summary card
            if (rev != null && !rev.isBlank()) {
                try { accumulatedRevenue += Double.parseDouble(rev); } catch (NumberFormatException ignored) {}
            }

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
        updateSummaryCards(rows.size());
    }

    private void updateSummaryCards(int reportCount) {
        if (totalReportsLabel != null) totalReportsLabel.setText(String.valueOf(reportCount));
        if (totalRevenueLabel != null) {
            totalRevenueLabel.setText(accumulatedRevenue > 0
                    ? "\u20b9" + String.format("%.0f", accumulatedRevenue)
                    : "\u20b90");
        }
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

            // Update stat cards from vehicle report response
            if (totalVehiclesLabel != null && total != null) totalVehiclesLabel.setText(total);
            if (availableVehiclesLabel != null && available != null) availableVehiclesLabel.setText(available);

            if (vehicleStatsLabel != null) {
                vehicleStatsLabel.setText(String.format(
                        "🚘 Fleet: Total=%s | ✅ Available=%s | 📅 Booked=%s | 🔧 Maintenance=%s",
                        nvl(total), nvl(available), nvl(booked), nvl(maintenance)));
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

    private String nvl(String s) {
        return s != null ? s : "0";
    }
}

