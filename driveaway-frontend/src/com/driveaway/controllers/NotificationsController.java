package com.driveaway.controllers;

import com.driveaway.services.NotificationService;
import com.driveaway.utils.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;

/**
 * NotificationsController - Displays notifications for the logged-in user.
 */
public class NotificationsController {

    @FXML private Label pageTitle;
    @FXML private TableView<String[]> notificationsTable;
    @FXML private TableColumn<String[], String> typeCol;
    @FXML private TableColumn<String[], String> subjectCol;
    @FXML private TableColumn<String[], String> messageCol;
    @FXML private TableColumn<String[], String> statusCol;
    @FXML private TableColumn<String[], String> sentAtCol;
    @FXML private Label statusLabel;

    private final NotificationService notificationService = new NotificationService();

    @FXML
    public void initialize() {
        setupTable();
        String userId = LoginController.getUserId();
        if (userId != null) {
            loadNotifications(userId);
        } else {
            setStatus("Please log in to view notifications.");
        }
    }

    private void setupTable() {
        if (typeCol != null) typeCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 0 ? d.getValue()[0] : ""));
        if (subjectCol != null) subjectCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 1 ? d.getValue()[1] : ""));
        if (messageCol != null) messageCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 2 ? shorten(d.getValue()[2], 60) : ""));
        if (statusCol != null) statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 3 ? d.getValue()[3] : ""));
        if (sentAtCol != null) sentAtCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 4 ? shorten(d.getValue()[4], 19) : ""));
    }

    private void loadNotifications(String userId) {
        String response = notificationService.getNotificationsForUser(userId);
        ObservableList<String[]> rows = FXCollections.observableArrayList();

        if (response == null || response.isBlank() || response.equals("[]")) {
            setStatus("No notifications found.");
            if (notificationsTable != null) notificationsTable.setItems(rows);
            return;
        }

        String[] entries = response.replace("[{", "").replace("}]", "").split("\\},\\{");
        for (String entry : entries) {
            if (entry.isBlank()) continue;
            String type = extractField(entry, "type");
            String subject = extractField(entry, "subject");
            String message = extractField(entry, "message");
            String status = extractField(entry, "status");
            String sentAt = extractField(entry, "sentAt");
            rows.add(new String[]{
                    type != null ? type : "",
                    subject != null ? subject : "",
                    message != null ? message : "",
                    status != null ? status : "",
                    sentAt != null ? sentAt : ""
            });
        }

        if (notificationsTable != null) notificationsTable.setItems(rows);
        setStatus(rows.size() + " notification(s) found.");
    }

    @FXML
    public void handleRefresh() {
        String userId = LoginController.getUserId();
        if (userId != null) loadNotifications(userId);
    }

    // Navigation
    @FXML public void goToDashboard() { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML public void goToVehicles() { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML public void goToBookings() { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML public void goToProfile() { SceneNavigator.load("views/UserProfileView.fxml"); }
    @FXML public void goToPayment() { SceneNavigator.load("views/PaymentView.fxml"); }
    @FXML public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private String extractField(String json, String field) {
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

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "\u2026" : s;
    }
}
