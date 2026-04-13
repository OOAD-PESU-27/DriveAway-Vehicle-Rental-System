package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.services.VehicleService;
import com.driveaway.utils.SceneNavigator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * StaffController — FINAL VERSION.
 * No hardcoded vehicle IDs. Loads handed-over bookings from backend.
 * Auto-fills Booking ID and Vehicle ID when staff clicks a row.
 */
public class StaffController {

    @FXML private Label staffNameLabel;
    @FXML private ListView<String> handoverList;
    @FXML private Label handoverStatusLabel;
    @FXML private TextField bookingIdField;
    @FXML private TextField vehicleIdField;
    @FXML private CheckBox hasDamageCheckBox;
    @FXML private VBox damageDetailsBox;
    @FXML private TextField damageDescriptionField;
    @FXML private ChoiceBox<String> damageSeverityChoice;
    @FXML private TextArea inspectionResultArea;
    @FXML private ListView<String> maintenanceList;
    @FXML private Label maintenanceStatusLabel;

    private final BookingService bookingService = new BookingService();
    private final VehicleService vehicleService = new VehicleService();

    // Raw data from handover list — each String[]: [bookingId, vehicleId, userId]
    private final List<String[]> handoverData = new ArrayList<>();
    private final List<String> maintenanceIds = new ArrayList<>();

    @FXML
    public void initialize() {
        // Show staff name
        staffNameLabel.setText("👤 " + LoginController.getUserName()
                + "  |  Role: " + LoginController.getUserRole());

        // Severity options
        damageSeverityChoice.setItems(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH"));
        damageSeverityChoice.setValue("MEDIUM");

        // Hide damage details by default
        if (damageDetailsBox != null) {
            damageDetailsBox.setVisible(false);
            damageDetailsBox.setManaged(false);
        }

        // Toggle damage box
        hasDamageCheckBox.selectedProperty().addListener((obs, o, newVal) -> {
            if (damageDetailsBox != null) {
                damageDetailsBox.setVisible(newVal);
                damageDetailsBox.setManaged(newVal);
            }
        });

        // Click handover row → auto-fill fields
        handoverList.getSelectionModel().selectedIndexProperty().addListener((obs, o, newIdx) -> {
            int idx = newIdx.intValue();
            if (idx >= 0 && idx < handoverData.size()) {
                String[] row = handoverData.get(idx);
                bookingIdField.setText(row[0]);
                vehicleIdField.setText(row[1]);
                if (handoverStatusLabel != null)
                    handoverStatusLabel.setText("Selected: Booking " + shorten(row[0])
                            + " | Vehicle " + shorten(row[1]));
            }
        });

        // Load on startup
        loadHandoverRequests();
    }

    @FXML
    public void handleRefresh() {
        loadHandoverRequests();
        if (inspectionResultArea != null) inspectionResultArea.setText("");
        if (maintenanceStatusLabel != null) maintenanceStatusLabel.setText("");
    }

    private void loadHandoverRequests() {
        handoverData.clear();
        handoverList.setItems(FXCollections.observableArrayList("Loading..."));

        new Thread(() -> {
            String response = bookingService.getBookingsByStatus("HANDED_OVER");
            Platform.runLater(() -> {
                if (response == null || response.equals("[]") || response.trim().isEmpty()) {
                    handoverList.setItems(FXCollections.observableArrayList(
                            "No pending handover requests."));
                    if (handoverStatusLabel != null)
                        handoverStatusLabel.setText("All clear — no vehicles currently handed over.");
                    return;
                }

                List<String> items = new ArrayList<>();
                String[] entries = splitJsonArray(response);
                for (String entry : entries) {
                    String id      = extractField(entry, "id");
                    String vId     = extractField(entry, "vehicleId");
                    String uId     = extractField(entry, "userId");
                    String total   = extractField(entry, "totalPrice");
                    String start   = extractField(entry, "startDate");
                    String end     = extractField(entry, "endDate");
                    if (id.isEmpty()) continue;

                    handoverData.add(new String[]{id, vId, uId, total, start, end});
                    items.add("📦 Booking: " + shorten(id)
                            + "  |  Vehicle: " + shorten(vId)
                            + "  |  Rs." + total
                            + "  |  " + start + " → " + end
                            + "  |  User: " + shorten(uId));
                }

                handoverList.setItems(FXCollections.observableArrayList(items));
                if (handoverStatusLabel != null)
                    handoverStatusLabel.setText(items.size() + " booking(s) pending return. Click a row to select.");
            });
        }).start();
    }

    @FXML
    public void handleSubmitInspection() {
        String bookingId = bookingIdField.getText().trim();
        String vehicleId = vehicleIdField.getText().trim();

        if (bookingId.isEmpty() || vehicleId.isEmpty()) {
            inspectionResultArea.setText(
                "⚠ Select a booking from the list above (it auto-fills the fields),\n"
                + "or manually type Booking ID and Vehicle ID.");
            return;
        }

        boolean hasDamage   = hasDamageCheckBox.isSelected();
        String  description = damageDescriptionField.getText().trim();
        String  severity    = damageSeverityChoice.getValue();

        if (hasDamage && description.isEmpty()) {
            inspectionResultArea.setText("⚠ Please describe the damage.");
            return;
        }

        String json = "{\"bookingId\":\"" + bookingId + "\","
                    + "\"vehicleId\":\"" + vehicleId + "\","
                    + "\"hasDamage\":" + hasDamage + ","
                    + "\"damageDescription\":\"" + description + "\","
                    + "\"damageSeverity\":\"" + (severity != null ? severity : "LOW") + "\"}";

        inspectionResultArea.setText("⏳ Submitting inspection...");

        new Thread(() -> {
            String response = bookingService.returnInspection(json);
            Platform.runLater(() -> {
                if (response != null && !response.isEmpty()) {
                    inspectionResultArea.setText("✅ " + response);
                    bookingIdField.clear();
                    vehicleIdField.clear();
                    damageDescriptionField.clear();
                    hasDamageCheckBox.setSelected(false);
                    damageSeverityChoice.setValue("MEDIUM");
                    loadHandoverRequests();
                } else {
                    inspectionResultArea.setText(
                        "❌ Inspection failed.\n"
                        + "Ensure booking is in HANDED_OVER status.\n"
                        + "Backend must be running on port 8080.");
                }
            });
        }).start();
    }

    @FXML
    public void handleLoadMaintenance() {
        maintenanceIds.clear();
        maintenanceList.setItems(FXCollections.observableArrayList("Loading..."));

        new Thread(() -> {
            String response = bookingService.getScheduledMaintenance();
            Platform.runLater(() -> {
                if (response == null || response.equals("[]") || response.trim().isEmpty()) {
                    maintenanceList.setItems(FXCollections.observableArrayList(
                            "No scheduled maintenance."));
                    return;
                }
                List<String> items = new ArrayList<>();
                String[] entries = splitJsonArray(response);
                for (String entry : entries) {
                    String id        = extractField(entry, "id");
                    String vehicleId = extractField(entry, "vehicleId");
                    String reason    = extractField(entry, "reason");
                    String scheduled = extractField(entry, "scheduledDate");
                    if (id.isEmpty()) continue;
                    maintenanceIds.add(id);
                    items.add("🔧 Vehicle: " + shorten(vehicleId)
                            + "  |  " + reason
                            + "  |  Scheduled: " + scheduled
                            + "  |  ID: " + shorten(id));
                }
                maintenanceList.setItems(FXCollections.observableArrayList(items));
                if (maintenanceStatusLabel != null)
                    maintenanceStatusLabel.setText(items.size() + " record(s) found.");
            });
        }).start();
    }

    @FXML
    public void handleCompleteMaintenance() {
        int idx = maintenanceList.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= maintenanceIds.size()) {
            if (maintenanceStatusLabel != null)
                maintenanceStatusLabel.setText("⚠ Select a maintenance record first.");
            return;
        }
        String maintenanceId = maintenanceIds.get(idx);

        new Thread(() -> {
            String response = bookingService.completeMaintenance(maintenanceId);
            Platform.runLater(() -> {
                if (response != null && response.contains("COMPLETED")) {
                    if (maintenanceStatusLabel != null)
                        maintenanceStatusLabel.setText(
                            "✅ Maintenance completed! Vehicle is AVAILABLE again.");
                    handleLoadMaintenance();
                } else {
                    if (maintenanceStatusLabel != null)
                        maintenanceStatusLabel.setText("Response: " + response);
                }
            });
        }).start();
    }

    @FXML
    public void handleLogout() {
        SceneNavigator.load("views/LoginView.fxml");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String[] splitJsonArray(String json) {
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]"))  trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.trim().isEmpty()) return new String[0];
        return trimmed.split("\\},\\{");
    }

    private String extractField(String json, String field) {
        try {
            String key = "\"" + field + "\":";
            int start = json.indexOf(key);
            if (start < 0) return "";
            start += key.length();
            char c = json.charAt(start);
            if (c == '"') {
                start++;
                int end = json.indexOf("\"", start);
                return end > start ? json.substring(start, end) : "";
            } else {
                int end = json.indexOf(",", start);
                if (end < 0) end = json.indexOf("}", start);
                return end > start ? json.substring(start, end).trim() : "";
            }
        } catch (Exception e) { return ""; }
    }

    private String shorten(String id) {
        if (id == null || id.length() <= 8) return id;
        return id.substring(0, 4) + "..." + id.substring(id.length() - 4);
    }
}