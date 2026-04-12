package com.driveaway.controllers;

import com.driveaway.services.NotificationService;
import com.driveaway.utils.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;

/**
 * NotificationsController - Displays notifications for the logged-in user.
 * Supports colour-coded type badges and mark-all-read action.
 */
public class NotificationsController {

    @FXML private Label pageTitle;
    @FXML private Label unreadCountLabel;
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
        if (typeCol != null) {
            typeCol.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().length > 0 ? d.getValue()[0] : ""));
            // Colour-coded notification type badge
            typeCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        String display = friendlyType(item);
                        Label badge = new Label(display);
                        badge.setStyle(getNotifTypeStyle(item));
                        setGraphic(badge);
                        setText(null);
                    }
                }
            });
        }
        if (subjectCol != null) subjectCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 1 ? d.getValue()[1] : ""));
        if (messageCol != null) {
            messageCol.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().length > 2 ? shorten(d.getValue()[2], 80) : ""));
            messageCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #1e293b; -fx-wrap-text: true;");
                    }
                }
            });
        }
        if (statusCol != null) {
            statusCol.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().length > 3 ? d.getValue()[3] : ""));
            statusCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    String upper = item.toUpperCase();
                    String style = switch (upper) {
                        case "SENT"     -> "-fx-text-fill: #059669; -fx-font-weight: bold;";
                        case "ACCEPTED" -> "-fx-text-fill: #1d4ed8; -fx-font-weight: bold;";
                        case "REJECTED" -> "-fx-text-fill: #dc2626; -fx-font-weight: bold;";
                        default         -> "-fx-text-fill: #6b7280;";
                    };
                    setText(item);
                    setStyle(style);
                }
            });
        }
        if (sentAtCol != null) sentAtCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().length > 4 ? shorten(d.getValue()[4], 19) : ""));
    }

    private void loadNotifications(String userId) {
        String response = notificationService.getNotificationsForUser(userId);
        ObservableList<String[]> rows = FXCollections.observableArrayList();

        if (response == null || response.isBlank() || response.equals("[]")) {
            setStatus("No notifications yet. Notifications will appear here after booking, payment, or return events.");
            if (notificationsTable != null) notificationsTable.setItems(rows);
            updateUnreadBadge(0);
            return;
        }

        String cleaned = response.trim();
        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("]"))   cleaned = cleaned.substring(0, cleaned.length() - 1);
        String[] entries = cleaned.split("\\},\\{");

        int unreadCount = 0;
        for (String raw : entries) {
            if (raw.isBlank()) continue;
            String entry = raw.startsWith("{") ? raw : "{" + raw;
            if (!entry.endsWith("}")) entry = entry + "}";

            String type    = extractField(entry, "type");
            String subject = extractField(entry, "subject");
            String message = extractField(entry, "message");
            String status  = extractField(entry, "status");
            String sentAt  = extractDateField(entry, "sentAt");
            String isRead  = extractBoolField(entry, "read");

            if ("false".equals(isRead) || isRead == null) unreadCount++;

            rows.add(new String[]{
                    type    != null ? type    : "",
                    subject != null ? subject : "",
                    message != null ? message : "",
                    status  != null ? status  : "SENT",
                    sentAt  != null ? sentAt  : ""
            });
        }

        if (notificationsTable != null) notificationsTable.setItems(rows);
        updateUnreadBadge(unreadCount);
        setStatus(rows.size() + " notification(s) | " + unreadCount + " unread");
    }

    @FXML
    public void handleRefresh() {
        String userId = LoginController.getUserId();
        if (userId != null) loadNotifications(userId);
    }

    @FXML
    public void handleMarkAllRead() {
        String userId = LoginController.getUserId();
        if (userId == null) return;
        String result = notificationService.markAllAsRead(userId);
        setStatus(result != null ? "✅ All notifications marked as read." : "❌ Failed to mark notifications as read.");
        loadNotifications(userId);
    }

    // Navigation
    @FXML public void goToDashboard()    { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML public void goToVehicles()     { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML public void goToBookings()     { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML public void goToProfile()      { SceneNavigator.load("views/UserProfileView.fxml"); }
    @FXML public void goToPayment()      { SceneNavigator.load("views/PaymentView.fxml"); }
    @FXML public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }

    private void updateUnreadBadge(int count) {
        if (unreadCountLabel != null) {
            unreadCountLabel.setText(count > 0 ? count + " unread" : "All read");
            unreadCountLabel.setStyle(count > 0
                    ? "-fx-text-fill: white; -fx-background-color: #e11d48; "
                      + "-fx-background-radius: 12; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 12px;"
                    : "-fx-text-fill: #15803d; -fx-background-color: #dcfce7; "
                      + "-fx-background-radius: 12; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 12px;");
        }
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    /** Translate enum value to a friendly short label. */
    private String friendlyType(String type) {
        if (type == null) return "";
        return switch (type.toUpperCase()) {
            case "BOOKING_CONFIRMED"           -> "✅ Booking Confirmed";
            case "BOOKING_CANCELLED"           -> "❌ Booking Cancelled";
            case "CANCELLATION_REFUND_PROCESSED" -> "💰 Refund Processed";
            case "PAYMENT_SUCCESS"             -> "💳 Payment Success";
            case "PAYMENT_COMPLETED"           -> "💳 Payment Completed";
            case "PAYMENT_FAILED"              -> "⚠️ Payment Failed";
            case "PAYMENT_APPROVED"            -> "✅ Payment Approved";
            case "PAYMENT_REQUEST_SENT"        -> "📤 Payment Request";
            case "REFUND_INITIATED"            -> "🔄 Refund Initiated";
            case "REFUND_COMPLETED"            -> "✅ Refund Completed";
            case "VEHICLE_RETURN_COMPLETED"    -> "🚗 Return Completed";
            case "DAMAGE_PENALTY_APPLIED"      -> "🔧 Damage Penalty";
            default -> type.replace("_", " ");
        };
    }

    /** Returns inline CSS for the notification type badge. */
    private String getNotifTypeStyle(String type) {
        String base = "-fx-padding: 3 8 3 8; -fx-background-radius: 10; "
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
        if (type == null) return base + "-fx-background-color: #6b7280;";
        return switch (type.toUpperCase()) {
            case "BOOKING_CONFIRMED"                  -> base + "-fx-background-color: #059669;";
            case "BOOKING_CANCELLED"                  -> base + "-fx-background-color: #dc2626;";
            case "CANCELLATION_REFUND_PROCESSED"      -> base + "-fx-background-color: #7c3aed;";
            case "PAYMENT_SUCCESS", "PAYMENT_COMPLETED", "PAYMENT_APPROVED" ->
                                                         base + "-fx-background-color: #2563eb;";
            case "PAYMENT_FAILED"                     -> base + "-fx-background-color: #f97316;";
            case "PAYMENT_REQUEST_SENT"               -> base + "-fx-background-color: #0284c7;";
            case "REFUND_INITIATED", "REFUND_COMPLETED" -> base + "-fx-background-color: #7c3aed;";
            case "VEHICLE_RETURN_COMPLETED"           -> base + "-fx-background-color: #0891b2;";
            case "DAMAGE_PENALTY_APPLIED"             -> base + "-fx-background-color: #b45309;";
            default                                   -> base + "-fx-background-color: #6b7280;";
        };
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

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
        if (ch == 'n') return null;  // null literal
        // number / boolean – read until comma or }
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        return end > start ? json.substring(start, end).trim() : null;
    }

    /**
     * Extract a date field that may be serialised as an ISO string or an array like [2024,4,15,10,30,0].
     * Returns a human-readable "YYYY-MM-DD HH:mm" string when possible.
     */
    private String extractDateField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        if (start >= json.length()) return null;
        char ch = json.charAt(start);
        if (ch == '"') {
            // ISO string: "2024-04-15T10:30:00"
            int end = json.indexOf('"', start + 1);
            String raw = end > start ? json.substring(start + 1, end) : null;
            return raw != null && raw.length() >= 16 ? raw.substring(0, 16).replace("T", " ") : raw;
        }
        if (ch == '[') {
            // Array [year, month, day, hour, minute, ...]
            int end = json.indexOf(']', start);
            if (end < 0) return null;
            String[] parts = json.substring(start + 1, end).split(",");
            if (parts.length >= 3) {
                String d = parts[0].trim() + "-"
                        + String.format("%02d", Integer.parseInt(parts[1].trim())) + "-"
                        + String.format("%02d", Integer.parseInt(parts[2].trim()));
                if (parts.length >= 5) {
                    d += " " + String.format("%02d", Integer.parseInt(parts[3].trim()))
                            + ":" + String.format("%02d", Integer.parseInt(parts[4].trim()));
                }
                return d;
            }
        }
        return null;
    }

    /** Extract a boolean field (returns "true" or "false"). */
    private String extractBoolField(String json, String field) {
        return extractField(json, field);
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "\u2026" : s;
    }
}
