package com.driveaway.controllers;

import com.driveaway.utils.HttpUtil;
import com.driveaway.utils.SceneNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminDashboardController - Admin fleet management, bookings, payment approval, and return processing.
 * Requires user to be logged in (uses their ID as admin ID for local testing).
 */
public class AdminDashboardController {

    // Fleet table
    @FXML private Label adminIdLabel;
    @FXML private Label fleetCountLabel;
    @FXML private Label bookingsCountLabel;
    @FXML private TableView<String[]> fleetTable;
    @FXML private TableColumn<String[], String> fleetIdCol;
    @FXML private TableColumn<String[], String> brandCol;
    @FXML private TableColumn<String[], String> modelCol;
    @FXML private TableColumn<String[], String> typeCol;
    @FXML private TableColumn<String[], String> priceCol;
    @FXML private TableColumn<String[], String> statusCol;
    @FXML private Label statusLabel;

    // Vehicle form fields
    @FXML private TextField vBrandField;
    @FXML private TextField vModelField;
    @FXML private ComboBox<String> vTypeCombo;
    @FXML private TextField vPriceField;
    @FXML private Label vehicleFormStatus;

    // Admin bookings table
    @FXML private TableView<String[]> adminBookingsTable;
    @FXML private TableColumn<String[], String> abIdCol;
    @FXML private TableColumn<String[], String> abUserCol;
    @FXML private TableColumn<String[], String> abVehicleCol;
    @FXML private TableColumn<String[], String> abStartCol;
    @FXML private TableColumn<String[], String> abEndCol;
    @FXML private TableColumn<String[], String> abStatusCol;
    @FXML private TableColumn<String[], String> abAmountCol;
    @FXML private Label adminBookingsStatus;

    // Payment approval
    @FXML private TextField approvePaymentIdField;
    @FXML private Label approvePaymentStatus;

    // Return / damage check
    @FXML private TextField returnBookingIdField;
    @FXML private TextField damageChargeField;
    @FXML private TextField damageNotesField;
    @FXML private Label returnStatus;

    private static final String BASE_URL = "http://localhost:8080";
    private String adminId;

    // Stores the ID of the vehicle selected in the fleet table (for update/delete)
    private String selectedVehicleId;

    @FXML
    public void initialize() {
        adminId = LoginController.getUserId();
        if (adminId == null) adminId = "ADMIN";
        if (adminIdLabel != null) adminIdLabel.setText("Admin ID: " + adminId);

        if (vTypeCombo != null) {
            vTypeCombo.getItems().addAll("CAR", "SUV", "BIKE", "VAN", "TRUCK", "BUS");
            vTypeCombo.setValue("CAR");
        }

        setupFleetTable();
        setupBookingsTable();
        loadFleet();
        loadAllBookingsAdmin();
    }

    // ── Fleet table ───────────────────────────────────────────────────────────

    private void setupFleetTable() {
        if (fleetIdCol != null) fleetIdCol.setCellValueFactory(d ->
                new SimpleStringProperty(shorten(d.getValue().length > 6 ? d.getValue()[6] : "", 12)));
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

        // Row selection → populate form fields
        if (fleetTable != null) {
            fleetTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
                if (row != null) {
                    selectedVehicleId = row.length > 6 ? row[6] : null;
                    if (vBrandField != null) vBrandField.setText(row.length > 0 ? row[0] : "");
                    if (vModelField != null) vModelField.setText(row.length > 1 ? row[1] : "");
                    if (vTypeCombo != null && row.length > 2) vTypeCombo.setValue(row[2]);
                    if (vPriceField != null) vPriceField.setText(row.length > 3 ? row[3] : "");
                }
            });
        }
    }

    private void loadFleet() {
        String response = HttpUtil.sendGetWithHeader(
                BASE_URL + "/api/v1/admin/fleet", "X-Admin-ID", adminId);
        if (response == null || response.isBlank() || "[]".equals(response)) {
            response = HttpUtil.sendGet(BASE_URL + "/api/v1/vehicles");
        }

        ObservableList<String[]> rows = FXCollections.observableArrayList(parseFleet(response));
        if (fleetTable != null) fleetTable.setItems(rows);
        if (fleetCountLabel != null) fleetCountLabel.setText(String.valueOf(rows.size()));
        setStatus(rows.size() + " vehicle(s) in fleet.");
    }

    private List<String[]> parseFleet(String response) {
        List<String[]> rows = new ArrayList<>();
        if (response == null || response.isBlank() || "[]".equals(response)) return rows;
        String[] entries = response.replace("[{", "").replace("}]", "").split("\\},\\{");
        for (String entry : entries) {
            if (entry.isBlank()) continue;
            rows.add(new String[]{
                extractField(entry, "brand"),
                extractField(entry, "model"),
                extractField(entry, "vehicleType"),
                extractField(entry, "pricePerDay"),
                valueOrDefault(extractField(entry, "status"), "AVAILABLE"),
                "", // placeholder
                valueOrDefault(extractField(entry, "id"), "")
            });
        }
        return rows;
    }

    @FXML
    public void handleRefreshFleet() {
        loadFleet();
    }

    // ── Vehicle CRUD ──────────────────────────────────────────────────────────

    @FXML
    public void handleAddVehicle() {
        String brand = vBrandField != null ? vBrandField.getText().trim() : "";
        String model = vModelField != null ? vModelField.getText().trim() : "";
        String type  = vTypeCombo != null ? vTypeCombo.getValue() : "";
        String price = vPriceField != null ? vPriceField.getText().trim() : "";

        if (brand.isBlank() || model.isBlank() || price.isBlank()) {
            setVehicleFormStatus("⚠️ Brand, model and price are required.", false);
            return;
        }
        double parsedPrice;
        try { parsedPrice = Double.parseDouble(price); } catch (NumberFormatException e) {
            setVehicleFormStatus("⚠️ Invalid price value.", false);
            return;
        }

        String json = buildVehicleJson(brand, model, type, parsedPrice);
        String response = HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/admin/fleet", json, "X-Admin-ID", adminId);
        if (response != null && !response.isBlank()) {
            setVehicleFormStatus("✅ Vehicle added successfully.", true);
            clearVehicleForm();
            loadFleet();
        } else {
            setVehicleFormStatus("❌ Failed to add vehicle.", false);
        }
    }

    @FXML
    public void handleUpdateVehicle() {
        if (selectedVehicleId == null || selectedVehicleId.isBlank()) {
            setVehicleFormStatus("⚠️ Please select a vehicle from the table first.", false);
            return;
        }
        String brand = vBrandField != null ? vBrandField.getText().trim() : "";
        String model = vModelField != null ? vModelField.getText().trim() : "";
        String type  = vTypeCombo != null ? vTypeCombo.getValue() : "";
        String price = vPriceField != null ? vPriceField.getText().trim() : "";

        if (brand.isBlank() || model.isBlank() || price.isBlank()) {
            setVehicleFormStatus("⚠️ Brand, model and price are required.", false);
            return;
        }
        double parsedPrice;
        try { parsedPrice = Double.parseDouble(price); } catch (NumberFormatException e) {
            setVehicleFormStatus("⚠️ Invalid price value.", false);
            return;
        }

        String json = buildVehicleJson(brand, model, type, parsedPrice);
        String response = HttpUtil.sendPutWithHeader(
                BASE_URL + "/api/v1/admin/fleet/" + selectedVehicleId, json, "X-Admin-ID", adminId);
        if (response != null && !response.isBlank()) {
            setVehicleFormStatus("✅ Vehicle updated successfully.", true);
            clearVehicleForm();
            loadFleet();
        } else {
            setVehicleFormStatus("❌ Failed to update vehicle.", false);
        }
    }

    private String buildVehicleJson(String brand, String model, String type, double pricePerDay) {
        return String.format(
                "{\"brand\":\"%s\",\"model\":\"%s\",\"vehicleType\":\"%s\",\"pricePerDay\":%.2f}",
                escape(brand), escape(model), escape(type), pricePerDay);
    }

    @FXML
    public void handleDeleteVehicle() {
        if (selectedVehicleId == null || selectedVehicleId.isBlank()) {
            setVehicleFormStatus("⚠️ Please select a vehicle from the table first.", false);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Vehicle");
        confirm.setHeaderText("Delete Vehicle");
        confirm.setContentText("Are you sure you want to delete vehicle " + shorten(selectedVehicleId, 16) + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String response = HttpUtil.sendDeleteWithHeader(
                        BASE_URL + "/api/v1/admin/fleet/" + selectedVehicleId, "X-Admin-ID", adminId);
                // DELETE returns 204 No Content on success (response may be empty)
                setVehicleFormStatus("✅ Vehicle deleted.", true);
                clearVehicleForm();
                selectedVehicleId = null;
                loadFleet();
            }
        });
    }

    private void clearVehicleForm() {
        if (vBrandField != null) vBrandField.clear();
        if (vModelField != null) vModelField.clear();
        if (vPriceField != null) vPriceField.clear();
        if (vTypeCombo != null) vTypeCombo.setValue("CAR");
        selectedVehicleId = null;
    }

    // ── Admin bookings table ──────────────────────────────────────────────────

    private void setupBookingsTable() {
        if (abIdCol != null) abIdCol.setCellValueFactory(d -> new SimpleStringProperty(
                shorten(d.getValue().length > 0 ? d.getValue()[0] : "", 16)));
        if (abUserCol != null) abUserCol.setCellValueFactory(d -> new SimpleStringProperty(
                shorten(d.getValue().length > 1 ? d.getValue()[1] : "", 14)));
        if (abVehicleCol != null) abVehicleCol.setCellValueFactory(d -> new SimpleStringProperty(
                shorten(d.getValue().length > 2 ? d.getValue()[2] : "", 14)));
        if (abStartCol != null) abStartCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().length > 3 ? d.getValue()[3] : ""));
        if (abEndCol != null) abEndCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().length > 4 ? d.getValue()[4] : ""));
        if (abStatusCol != null) abStatusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().length > 5 ? d.getValue()[5] : ""));
        if (abAmountCol != null) abAmountCol.setCellValueFactory(d -> {
            String amt = d.getValue().length > 6 ? d.getValue()[6] : "0";
            try { return new SimpleStringProperty("\u20b9" + String.format("%,.2f", Double.parseDouble(amt))); }
            catch (NumberFormatException e) { return new SimpleStringProperty("\u20b9" + amt); }
        });
    }

    @FXML
    public void loadAllBookingsAdmin() {
        loadBookings(BASE_URL + "/api/v1/bookings", "all");
    }

    @FXML
    public void loadActiveBookingsAdmin() {
        loadBookings(BASE_URL + "/api/v1/bookings?status=active", "active");
    }

    @FXML
    public void loadCompletedBookingsAdmin() {
        loadBookings(BASE_URL + "/api/v1/bookings?status=completed", "completed");
    }

    @FXML
    public void handleRefreshBookings() {
        loadAllBookingsAdmin();
    }

    private void loadBookings(String url, String filter) {
        if (adminBookingsStatus != null) adminBookingsStatus.setText("Loading bookings...");
        String response = HttpUtil.sendGet(url);
        List<String[]> rows = new ArrayList<>();
        if (response != null && !response.isBlank() && !"[]".equals(response)) {
            String[] entries = response.replace("[", "").replace("]", "").split("\\},\\{");
            for (String entry : entries) {
                if (entry.isBlank()) continue;
                String id       = extractField(entry, "id");
                String userId   = extractField(entry, "userId");
                String vehicleId = extractField(entry, "vehicleId");
                String start    = extractField(entry, "startDate");
                String end      = extractField(entry, "endDate");
                String status   = extractField(entry, "status");
                String paid     = extractField(entry, "paidAmount");
                String total    = extractField(entry, "totalPrice");
                String amount = "0";
                if (paid != null) { try { if (Double.parseDouble(paid) > 0) amount = paid; } catch (NumberFormatException ignored) {} }
                if ("0".equals(amount) && total != null) amount = total;
                if (id != null) {
                    rows.add(new String[]{
                        id,
                        valueOrDefault(userId, "-"),
                        valueOrDefault(vehicleId, "-"),
                        valueOrDefault(start, "-"),
                        valueOrDefault(end, "-"),
                        valueOrDefault(status, "PENDING"),
                        amount
                    });
                }
            }
        }
        if (adminBookingsTable != null) {
            adminBookingsTable.setItems(FXCollections.observableArrayList(rows));
        }
        if (bookingsCountLabel != null) bookingsCountLabel.setText(String.valueOf(rows.size()));
        if (adminBookingsStatus != null) {
            adminBookingsStatus.setText(rows.size() + " booking(s) [" + filter + "]");
        }
    }

    // ── Payment approval ──────────────────────────────────────────────────────

    @FXML
    public void handleAdminApprovePayment() {
        String paymentId = approvePaymentIdField != null ? approvePaymentIdField.getText().trim() : "";
        if (paymentId.isBlank()) {
            setApproveStatus("⚠️ Payment ID is required.", false);
            return;
        }
        String response = HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/payments/" + paymentId + "/approve",
                "{}", "X-Approved-By", adminId);
        if (response != null && response.contains("\"success\":true")) {
            setApproveStatus("✅ Payment " + shorten(paymentId, 16) + " approved.", true);
            if (approvePaymentIdField != null) approvePaymentIdField.clear();
        } else {
            setApproveStatus("❌ Could not approve payment. Check the ID and try again.", false);
        }
    }

    // ── Return / Damage Check ─────────────────────────────────────────────────

    @FXML
    public void handleProcessReturn() {
        String bookingId = returnBookingIdField != null ? returnBookingIdField.getText().trim() : "";
        String damageChargeStr = damageChargeField != null ? damageChargeField.getText().trim() : "0";
        String notes = damageNotesField != null ? damageNotesField.getText().trim() : "";

        if (bookingId.isBlank()) {
            setReturnStatus("⚠️ Booking ID is required.", false);
            return;
        }

        double damageCharge = 0;
        try { damageCharge = Double.parseDouble(damageChargeStr); } catch (NumberFormatException ignored) {}

        String json = String.format(
                "{\"damageNotes\":\"%s\",\"damageCharge\":%.2f}",
                escape(notes), damageCharge);
        String response = HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/bookings/" + bookingId + "/return",
                json, "X-Staff-ID", adminId);
        if (response != null && response.contains("\"status\":\"RETURNED\"")) {
            setReturnStatus("✅ Return processed. Deposit refund/forfeiture applied.", true);
            if (returnBookingIdField != null) returnBookingIdField.clear();
            if (damageChargeField != null) damageChargeField.clear();
            if (damageNotesField != null) damageNotesField.clear();
            loadAllBookingsAdmin();
        } else {
            String errMsg = response != null && response.contains("\"") ? response : "Return processing failed.";
            setReturnStatus("❌ " + errMsg, false);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML public void goToReports() { SceneNavigator.load("views/ReportsView.fxml"); }
    @FXML public void goToNotifications() { SceneNavigator.load("views/NotificationsView.fxml"); }
    @FXML public void goToDashboard() { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML public void goToVehicles() { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML public void goToBookings() { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML public void goToProfile() { SceneNavigator.load("views/UserProfileView.fxml"); }
    @FXML public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }

    // ── Status helpers ────────────────────────────────────────────────────────

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void setVehicleFormStatus(String msg, boolean ok) {
        if (vehicleFormStatus != null) {
            vehicleFormStatus.getStyleClass().removeAll("text-success", "text-danger", "text-muted");
            vehicleFormStatus.getStyleClass().add(ok ? "text-success" : "text-danger");
            vehicleFormStatus.setText(msg);
        }
    }

    private void setApproveStatus(String msg, boolean ok) {
        if (approvePaymentStatus != null) {
            approvePaymentStatus.getStyleClass().removeAll("text-success", "text-danger", "text-muted");
            approvePaymentStatus.getStyleClass().add(ok ? "text-success" : "text-danger");
            approvePaymentStatus.setText(msg);
        }
    }

    private void setReturnStatus(String msg, boolean ok) {
        if (returnStatus != null) {
            returnStatus.getStyleClass().removeAll("text-success", "text-danger", "text-muted");
            returnStatus.getStyleClass().add(ok ? "text-success" : "text-danger");
            returnStatus.setText(msg);
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private String valueOrDefault(String value, String def) {
        return (value != null && !value.isBlank()) ? value : def;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
