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
 * BookingController — FINAL VERSION.
 *
 * KEY FIX: Vehicles are now loaded DYNAMICALLY from the backend API.
 * No more hardcoded vehicle IDs that break when vehicles change.
 *
 * Flow:
 *   initialize() → calls loadVehiclesFromBackend() on a background thread
 *   → parses the JSON response → fills vehicleComboBox with real names
 *   → stores vehicleIds[] for lookup when booking is submitted
 */
public class BookingController {

    // ── Customer Booking Section ──────────────────────────────────────────────
    @FXML private ComboBox<String> vehicleComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private CheckBox licenseVerifiedCheckBox;
    @FXML private Label bookingResultLabel;
    @FXML private Label welcomeLabel;

    // ── My Bookings Section ───────────────────────────────────────────────────
    @FXML private VBox myBookingsSection;
    @FXML private ListView<String> myBookingsList;
    @FXML private Label handoverResultLabel;

    // ── Staff Inspection Section ──────────────────────────────────────────────
    @FXML private VBox staffInspectionSection;
    @FXML private ListView<String> handoverRequestsList;
    @FXML private TextField bookingIdField;
    @FXML private TextField returnVehicleIdField;
    @FXML private CheckBox hasDamageCheckBox;
    @FXML private TextField damageDescriptionField;
    @FXML private ChoiceBox<String> damageSeverityChoice;
    @FXML private TextArea inspectionResultArea;

    private final BookingService bookingService = new BookingService();
    private final VehicleService vehicleService = new VehicleService();

    // Dynamically loaded from backend — parallel arrays
    // vehicleDisplayNames[i] corresponds to vehicleIds[i]
    private final List<String> vehicleDisplayNames = new ArrayList<>();
    private final List<String> vehicleIds = new ArrayList<>();

    // Booking data loaded from "My Bookings"
    // Each String[]: [bookingId, vehicleId, status, totalPrice, startDate, endDate]
    private final List<String[]> myBookingData = new ArrayList<>();

    // ── initialize() — runs after FXML loads ─────────────────────────────────
    @FXML
    public void initialize() {
        // ══ ROLE-BASED VIEW ══════════════════════════════════════════════════
        // CUSTOMER should NOT see staff section
        String userRole = LoginController.getUserRole();
        System.out.println("[BOOKING] User Role: " + userRole);
        System.out.println("[BOOKING] Staff section reference: " + staffInspectionSection);
        
        if ("CUSTOMER".equalsIgnoreCase(userRole)) {
            // Hide staff inspection section completely from customers
            if (staffInspectionSection != null) {
                staffInspectionSection.setVisible(false);
                staffInspectionSection.setManaged(false);
                System.out.println("[BOOKING] Staff section HIDDEN for customer");
            } else {
                System.out.println("[BOOKING] ERROR: staffInspectionSection is NULL!");
            }
        } else {
            System.out.println("[BOOKING] Staff section shown for role: " + userRole);
        }
        
        // Severity options for inspection section
        damageSeverityChoice.setItems(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH"));
        damageSeverityChoice.setValue("MEDIUM");

        // Welcome label
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + LoginController.getUserEmail());
        }

        // Hide "My Bookings" section initially — shown when button clicked
        if (myBookingsSection != null) {
            myBookingsSection.setVisible(false);
            myBookingsSection.setManaged(false);
        }

        // When staff selects from handover list → auto-fill fields
        if (handoverRequestsList != null) {
            handoverRequestsList.getSelectionModel().selectedIndexProperty()
                    .addListener((obs, oldIdx, newIdx) -> {
                        int idx = newIdx.intValue();
                        if (idx >= 0 && idx < handoverRawData.size()) {
                            String[] row = handoverRawData.get(idx);
                            bookingIdField.setText(row[0]);
                            returnVehicleIdField.setText(row[1]);
                        }
                    });
        }

        // Load vehicles from backend on background thread
        loadVehiclesFromBackend();

        // Load handed-over bookings for staff section
        loadHandoverRequests();
    }

    // ── Load vehicles dynamically from backend ────────────────────────────────

    private void loadVehiclesFromBackend() {
        vehicleComboBox.setItems(FXCollections.observableArrayList("Loading vehicles..."));
        vehicleComboBox.getSelectionModel().selectFirst();

        new Thread(() -> {
            String response = vehicleService.getAvailableVehicles();

            Platform.runLater(() -> {
                vehicleDisplayNames.clear();
                vehicleIds.clear();

                if (response == null || response.equals("[]") || response.trim().isEmpty()) {
                    vehicleComboBox.setItems(FXCollections.observableArrayList(
                            "No vehicles available — check backend"));
                    return;
                }

                // Parse vehicle JSON array
                String[] entries = splitJsonArray(response);
                for (String entry : entries) {
                    String id     = extractField(entry, "id");
                    String brand  = extractField(entry, "brand");
                    String model  = extractField(entry, "model");
                    String type   = extractField(entry, "vehicleType");
                    String price  = extractField(entry, "pricePerDay");
                    String avail  = extractField(entry, "available");

                    if (id.isEmpty()) continue;
                    if ("false".equals(avail)) continue; // skip unavailable

                    String displayName = brand + " " + model
                            + " (" + type + ") — Rs." + price + "/day";
                    vehicleDisplayNames.add(displayName);
                    vehicleIds.add(id);
                }

                if (vehicleDisplayNames.isEmpty()) {
                    vehicleComboBox.setItems(FXCollections.observableArrayList(
                            "No available vehicles — check MongoDB vehicles collection"));
                } else {
                    vehicleComboBox.setItems(FXCollections.observableArrayList(vehicleDisplayNames));
                    vehicleComboBox.getSelectionModel().selectFirst();
                }
            });
        }).start();
    }

    // ── FEATURE 1: Book a Vehicle ─────────────────────────────────────────────
    @FXML
    public void handleBookVehicle() {
        int idx = vehicleComboBox.getSelectionModel().getSelectedIndex();

        if (idx < 0 || idx >= vehicleIds.size()) {
            setResult(bookingResultLabel, "Please select a vehicle.", false);
            return;
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            setResult(bookingResultLabel, "Please select start and end dates.", false);
            return;
        }

        if (!licenseVerifiedCheckBox.isSelected()) {
            setResult(bookingResultLabel, "Please confirm your license is verified.", false);
            return;
        }

        String vehicleId   = vehicleIds.get(idx);
        String vehicleName = vehicleDisplayNames.get(idx);
        String userId      = LoginController.getUserId();
        String startDate   = startDatePicker.getValue().toString();
        String endDate     = endDatePicker.getValue().toString();

        String json = "{\"userId\":\"" + userId + "\","
                    + "\"vehicleId\":\"" + vehicleId + "\","
                    + "\"startDate\":\"" + startDate + "\","
                    + "\"endDate\":\"" + endDate + "\"}";

        setResult(bookingResultLabel, "⏳ Creating booking...", true);
        bookingResultLabel.setStyle("-fx-text-fill: #a8a8b3; -fx-font-size: 13;");

        new Thread(() -> {
            String response = bookingService.createBooking(json, true);
            Platform.runLater(() -> {
                if (response != null && response.contains("\"id\"")) {
                    String bookingId   = extractField(response, "id");
                    String totalPrice  = extractField(response, "totalPrice");
                    setResult(bookingResultLabel,
                        "✅ Booking confirmed!\n"
                        + "Vehicle: " + vehicleName + "\n"
                        + "Booking ID: " + bookingId + "\n"
                        + "Total: Rs." + totalPrice
                        + "\n\nNow click 'My Bookings' to see it.",
                        true);
                    // Refresh vehicle list (booked vehicle will disappear)
                    loadVehiclesFromBackend();
                } else {
                    setResult(bookingResultLabel,
                        "❌ " + (response != null ? response : "Backend not reachable"),
                        false);
                }
            });
        }).start();
    }

    // ── FEATURE 2: View My Bookings ───────────────────────────────────────────
    @FXML
    public void handleViewMyBookings() {
        myBookingsSection.setVisible(true);
        myBookingsSection.setManaged(true);
        myBookingData.clear();

        String userId = LoginController.getUserId();
        myBookingsList.setItems(FXCollections.observableArrayList("Loading..."));

        new Thread(() -> {
            String response = bookingService.getBookingsByUser(userId);
            Platform.runLater(() -> {
                if (response == null || response.equals("[]") || response.trim().isEmpty()) {
                    myBookingsList.setItems(FXCollections.observableArrayList("No bookings found."));
                    return;
                }

                List<String> items = new ArrayList<>();
                String[] entries = splitJsonArray(response);

                for (String entry : entries) {
                    String id       = extractField(entry, "id");
                    String vId      = extractField(entry, "vehicleId");
                    String status   = extractField(entry, "status");
                    String total    = extractField(entry, "totalPrice");
                    String start    = extractField(entry, "startDate");
                    String end      = extractField(entry, "endDate");
                    if (id.isEmpty()) continue;

                    myBookingData.add(new String[]{id, vId, status, total, start, end});
                    items.add(status + "  |  Rs." + total
                            + "  |  " + start + " → " + end
                            + "  |  ID: " + shorten(id));
                }

                myBookingsList.setItems(FXCollections.observableArrayList(items));
                if (handoverResultLabel != null) handoverResultLabel.setText("");
            });
        }).start();
    }

    // ── FEATURE 3: Request Handover ───────────────────────────────────────────
    @FXML
    public void handleRequestHandover() {
        int idx = myBookingsList.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= myBookingData.size()) {
            if (handoverResultLabel != null)
                handoverResultLabel.setText("Select a booking from the list first.");
            return;
        }

        String[] row = myBookingData.get(idx);
        String bookingId = row[0];
        String status    = row[2];

        if (!"CONFIRMED".equalsIgnoreCase(status)) {
            if (handoverResultLabel != null)
                handoverResultLabel.setText("Only CONFIRMED bookings can be handed over. This is: " + status);
            return;
        }

        new Thread(() -> {
            String response = bookingService.handoverVehicle(bookingId);
            Platform.runLater(() -> {
                if (response != null && response.contains("HANDED_OVER")) {
                    if (handoverResultLabel != null) {
                        handoverResultLabel.setText(
                            "✅ Handover done! Staff can now see this in their dashboard.");
                        handoverResultLabel.setStyle("-fx-text-fill: #4ecca3;");
                    }
                    handleViewMyBookings(); // Refresh
                } else {
                    if (handoverResultLabel != null)
                        handoverResultLabel.setText("Error: " + (response != null ? response : "Backend unreachable"));
                }
            });
        }).start();
    }

    // ── FEATURE 4: Staff Inspection (kept here for backward compat) ───────────
    private final List<String[]> handoverRawData = new ArrayList<>();

    private void loadHandoverRequests() {
        if (handoverRequestsList == null) return;
        handoverRawData.clear();

        new Thread(() -> {
            String response = bookingService.getBookingsByStatus("HANDED_OVER");
            Platform.runLater(() -> {
                if (response == null || response.equals("[]")) {
                    handoverRequestsList.setItems(FXCollections.observableArrayList(
                            "No pending handovers."));
                    return;
                }
                List<String> items = new ArrayList<>();
                String[] entries = splitJsonArray(response);
                for (String entry : entries) {
                    String id  = extractField(entry, "id");
                    String vid = extractField(entry, "vehicleId");
                    String uid = extractField(entry, "userId");
                    if (id.isEmpty()) continue;
                    handoverRawData.add(new String[]{id, vid, uid});
                    items.add("HANDED_OVER | ID: " + shorten(id)
                            + " | VehicleId: " + shorten(vid)
                            + " | User: " + shorten(uid));
                }
                handoverRequestsList.setItems(FXCollections.observableArrayList(items));
            });
        }).start();
    }

    @FXML
    public void handleReturnInspection() {
        String bookingId = bookingIdField.getText().trim();
        String vehicleId = returnVehicleIdField.getText().trim();
        if (bookingId.isEmpty() || vehicleId.isEmpty()) {
            inspectionResultArea.setText("Fill Booking ID and Vehicle ID.");
            return;
        }
        boolean hasDamage   = hasDamageCheckBox.isSelected();
        String  description = damageDescriptionField.getText().trim();
        String  severity    = damageSeverityChoice.getValue();

        String json = "{\"bookingId\":\"" + bookingId + "\","
                    + "\"vehicleId\":\"" + vehicleId + "\","
                    + "\"hasDamage\":" + hasDamage + ","
                    + "\"damageDescription\":\"" + description + "\","
                    + "\"damageSeverity\":\"" + (severity != null ? severity : "LOW") + "\"}";

        new Thread(() -> {
            String response = bookingService.returnInspection(json);
            Platform.runLater(() -> {
                if (response != null) {
                    inspectionResultArea.setText("✅ " + response);
                    loadHandoverRequests();
                    loadVehiclesFromBackend();
                } else {
                    inspectionResultArea.setText("❌ Backend not reachable.");
                }
            });
        }).start();
    }

    @FXML
    public void handleLogout() {
        SceneNavigator.load("views/LoginView.fxml");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Split JSON array string into individual object strings */
    private String[] splitJsonArray(String json) {
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]"))  trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.trim().isEmpty()) return new String[0];
        return trimmed.split("\\},\\{");
    }

    /** Extract a field value from a JSON fragment */
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

    private void setResult(Label label, String msg, boolean success) {
        if (label == null) return;
        label.setText(msg);
        label.setStyle(success
            ? "-fx-text-fill: #4ecca3; -fx-font-size: 13; -fx-font-weight: bold;"
            : "-fx-text-fill: #e94560; -fx-font-size: 13;");
    }
}