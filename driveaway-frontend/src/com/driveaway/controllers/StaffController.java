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

    private final List<String[]> handoverData = new ArrayList<>();
    private final List<String> maintenanceIds = new ArrayList<>();

    @FXML
    public void initialize() {
        staffNameLabel.setText(
            "👤 " + LoginController.getUserName()
            + "  |  Role: " + LoginController.getUserRole()
        );

        damageSeverityChoice.setItems(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH"));
        damageSeverityChoice.setValue("MEDIUM");

        damageDetailsBox.setVisible(false);
        damageDetailsBox.setManaged(false);

        hasDamageCheckBox.selectedProperty().addListener((obs, o, val) -> {
            damageDetailsBox.setVisible(val);
            damageDetailsBox.setManaged(val);
        });

        handoverList.getSelectionModel().selectedIndexProperty().addListener((obs, o, newIdx) -> {
            int idx = newIdx.intValue();
            if (idx >= 0 && idx < handoverData.size()) {
                String[] row = handoverData.get(idx);
                bookingIdField.setText(row[0]);
                vehicleIdField.setText(row[1]);
                if (handoverStatusLabel != null)
                    handoverStatusLabel.setText("Selected: Booking " + shorten(row[0]) + " | Vehicle " + shorten(row[1]));
            }
        });

        loadHandoverRequests();
    }

    // ── THIS WAS MISSING — StaffView.fxml calls #handleRefresh ───────────────
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
                    handoverList.setItems(FXCollections.observableArrayList("No pending handover requests."));
                    if (handoverStatusLabel != null)
                        handoverStatusLabel.setText("No vehicles currently handed over.");
                    return;
                }

                List<String> items = new ArrayList<>();
                for (String entry : splitJsonArray(response)) {
                    String id  = extractField(entry, "id");
                    String vId = extractField(entry, "vehicleId");
                    String uid = extractField(entry, "userId");

                    if (id.isEmpty()) continue;

                    handoverData.add(new String[]{id, vId, uid});
                    items.add("📦 Booking: " + shorten(id) + " | Vehicle: " + shorten(vId) + " | User: " + shorten(uid));
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

        if (bookingId.isBlank() || vehicleId.isBlank()) {
            inspectionResultArea.setText("❌ Select a booking from the list above first.");
            return;
        }

        boolean hasDamage  = hasDamageCheckBox.isSelected();
        String description = damageDescriptionField.getText().trim();
        String severity    = damageSeverityChoice.getValue();

        if (hasDamage && description.isBlank()) {
            inspectionResultArea.setText("⚠ Please describe the damage.");
            return;
        }

        String json = "{\"bookingId\":\"" + bookingId + "\","
                    + "\"vehicleId\":\"" + vehicleId + "\","
                    + "\"hasDamage\":" + hasDamage + ","
                    + "\"damageDescription\":\"" + description + "\","
                    + "\"damageSeverity\":\"" + (severity != null ? severity : "LOW") + "\"}";

        inspectionResultArea.setText("⏳ Submitting...");

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
                } else {
                    inspectionResultArea.setText("❌ Failed. Ensure booking is HANDED_OVER and backend is running.");
                }
                loadHandoverRequests();
            });
        }).start();
    }

    @FXML
    public void handleLoadMaintenance() {
        maintenanceIds.clear();
        if (maintenanceList != null)
            maintenanceList.setItems(FXCollections.observableArrayList("Loading..."));

        new Thread(() -> {
            String response = bookingService.getScheduledMaintenance();
            Platform.runLater(() -> {
                List<String> items = new ArrayList<>();

                if (response == null || response.equals("[]") || response.trim().isEmpty()) {
                    if (maintenanceList != null)
                        maintenanceList.setItems(FXCollections.observableArrayList("No scheduled maintenance."));
                    return;
                }

                for (String entry : splitJsonArray(response)) {
                    String id          = extractField(entry, "id");
                    String vehicleId   = extractField(entry, "vehicleId");
                    String description = extractField(entry, "description");
                    String scheduled   = extractField(entry, "scheduledDate");

                    if (id.isEmpty()) continue;

                    maintenanceIds.add(id);
                    items.add("🔧 Vehicle: " + shorten(vehicleId)
                            + " | " + (description.isEmpty() ? "Maintenance" : description)
                            + " | Scheduled: " + scheduled);
                }

                if (maintenanceList != null)
                    maintenanceList.setItems(FXCollections.observableArrayList(items));
                if (maintenanceStatusLabel != null)
                    maintenanceStatusLabel.setText(items.size() + " record(s) found.");
            });
        }).start();
    }

    @FXML
    public void handleCompleteMaintenance() {
        if (maintenanceList == null) return;
        int idx = maintenanceList.getSelectionModel().getSelectedIndex();

        if (idx < 0 || idx >= maintenanceIds.size()) {
            if (maintenanceStatusLabel != null)
                maintenanceStatusLabel.setText("⚠ Select a maintenance record first.");
            return;
        }

        String id = maintenanceIds.get(idx);

        new Thread(() -> {
            String response = bookingService.completeMaintenance(id);
            Platform.runLater(() -> {
                if (maintenanceStatusLabel != null)
                    maintenanceStatusLabel.setText(response != null ? "✅ " + response : "❌ Failed.");
                handleLoadMaintenance();
            });
        }).start();
    }

    @FXML
    public void handleLogout() {
        SceneNavigator.load("views/LoginView.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String[] splitJsonArray(String json) {
        if (json == null || json.length() < 2) return new String[0];
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
            if (start >= json.length()) return "";
            char c = json.charAt(start);
            if (c == '"') {
                start++;
                int end = json.indexOf("\"", start);
                return end < 0 ? "" : json.substring(start, end);
            } else {
                int end = json.indexOf(",", start);
                if (end < 0) end = json.indexOf("}", start);
                return end < 0 ? "" : json.substring(start, end).trim();
            }
        } catch (Exception e) { return ""; }
    }

    private String shorten(String id) {
        if (id == null || id.length() < 8) return id != null ? id : "";
        return id.substring(0, 4) + "..." + id.substring(id.length() - 4);
    }
}