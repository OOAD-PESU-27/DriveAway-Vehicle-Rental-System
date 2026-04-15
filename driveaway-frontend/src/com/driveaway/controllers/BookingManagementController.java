package com.driveaway.controllers;

import com.driveaway.services.BookingService;
import com.driveaway.utils.SceneNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;

/**
 * BookingManagementController - Manages the bookings list page
 * Displays user bookings with filter and cancel options
 */
public class BookingManagementController {

    @FXML private TableView<String[]> bookingsTable;
    @FXML private TableColumn<String[], String> bookingIdCol;
    @FXML private TableColumn<String[], String> vehicleIdCol;
    @FXML private TableColumn<String[], String> startDateCol;
    @FXML private TableColumn<String[], String> endDateCol;
    @FXML private TableColumn<String[], String> statusCol;
    @FXML private TableColumn<String[], String> amountCol;
    @FXML private TableColumn<String[], String> actionCol;
    @FXML private Label statusLabel;
    @FXML private Label bookingCountLabel;
    @FXML private ToggleButton allBtn;
    @FXML private ToggleButton activeBtn;
    @FXML private ToggleButton completedBtn;
    @FXML private ToggleButton cancelledBtn;

    private final BookingService bookingService = new BookingService();
    private List<String[]> allBookings = new ArrayList<>();
    private static String lastBookingId;
    private static double lastBookingTotalPrice;

    public static void setLastBookingId(String id) {
        lastBookingId = id;
    }

    public static String getLastBookingId() {
        return lastBookingId;
    }

    public static void setLastBookingTotalPrice(double price) {
        lastBookingTotalPrice = price;
    }

    public static double getLastBookingTotalPrice() {
        return lastBookingTotalPrice;
    }

    @FXML
    public void initialize() {
        setupTable();
        loadBookings();
    }

    private void setupTable() {
        bookingIdCol.setCellValueFactory(data -> new SimpleStringProperty(
                shorten(data.getValue()[0], 16)));
        vehicleIdCol.setCellValueFactory(data -> new SimpleStringProperty(
                shorten(data.getValue()[1], 22)));
        startDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2]));
        endDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3]));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[4]));
        amountCol.setCellValueFactory(data -> {
            String amountStr = data.getValue()[5];
            try {
                double amt = Double.parseDouble(amountStr);
                return new SimpleStringProperty("₹" + String.format("%,.2f", amt));
            } catch (NumberFormatException e) {
                return new SimpleStringProperty("₹" + amountStr);
            }
        });

        // Action column with Cancel button
        actionCol.setCellFactory(col -> new TableCell<>() {

             private final Button cancelBtn = new Button("Cancel");
             private final Button handoverBtn = new Button("🚀 Request Handover");
             private final Label statusLabel = new Label();

            {
            cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");

            cancelBtn.setOnAction(e -> {
                String[] row = getTableView().getItems().get(getIndex());
                handleCancel(row[0], row[4], row[2]);
            });

            handoverBtn.getStyleClass().addAll("btn-primary", "btn-small");

            handoverBtn.setOnAction(e -> {
                String[] row = getTableView().getItems().get(getIndex());
                String bookingId = row[0];

                String result = bookingService.handoverVehicle(bookingId);

                if (result != null && result.contains("HANDED_OVER")) {
                    setStatus("✅ Handover requested successfully.");
                    loadBookings(); // refresh table
                } else {
                    setStatus("❌ Failed to request handover.");
                }
            });
        
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
            return;
        }

        String[] row = getTableView().getItems().get(getIndex());
        String status = row[4];

        switch (status.toUpperCase()) {

            case "CONFIRMED":
                setGraphic(handoverBtn);
                break;

            case "HANDED_OVER":
                statusLabel.setText("⏳ Waiting for inspection");
                statusLabel.setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
                setGraphic(statusLabel);
                break;

            case "RETURNED":
            case "COMPLETED":
                statusLabel.setText("✅ Completed");
                statusLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                setGraphic(statusLabel);
                break;

            case "ACTIVE":
            case "PENDING":
                cancelBtn.setDisable(false);
                setGraphic(cancelBtn);
                break;

            default:
                setGraphic(null);
        }
    }
});

        
    }

    @FXML
    public void handleRefresh() {
        loadBookings();
    }

    private void loadBookings() {
        String userId = LoginController.getUserId();
        if (userId == null) {
            setStatus("Please log in to view bookings.");
            return;
        }

        setStatus("Loading bookings...");
        allBookings.clear();

        String response = bookingService.getUserBookings(userId);
        if (response == null || response.isBlank() || response.equals("[]")) {
            setStatus("");
            updateTable(allBookings);
            return;
        }

        String[] entries = response.replace("[", "").replace("]", "").split("\\},\\{");
        for (String entry : entries) {
            String id = extract(entry, "id");
            String vehicleId = extract(entry, "vehicleId");
            String start = extract(entry, "startDate");
            String end = extract(entry, "endDate");
            String status = extract(entry, "status");

            // Prefer paidAmount (set after payment completes) over totalPrice (estimated at booking time)
            String paidAmountStr = extract(entry, "paidAmount");
            String totalPriceStr = extract(entry, "totalPrice");
            String amount = "0";
            if (paidAmountStr != null) {
                try {
                    double paid = Double.parseDouble(paidAmountStr);
                    if (paid > 0) amount = paidAmountStr;
                } catch (NumberFormatException ignored) {
                    // paidAmountStr was not a valid number; fall through to totalPrice fallback
                }
            }
            if ("0".equals(amount) && totalPriceStr != null) {
                amount = totalPriceStr;
            }

            if (id != null) {
                allBookings.add(new String[]{
                        id,
                        vehicleId != null ? vehicleId : "-",
                        start != null ? start : "-",
                        end != null ? end : "-",
                        status != null ? status : "PENDING",
                        amount
                });
            }
        }

        setStatus("");
        updateTable(allBookings);
    }

    private void handleCancel(String bookingId, String status, String startDateStr) {
        if (!"ACTIVE".equalsIgnoreCase(status) && !"CONFIRMED".equalsIgnoreCase(status)
                && !"PENDING".equalsIgnoreCase(status)) {
            setStatus("This booking cannot be cancelled.");
            return;
        }

        // Compute days until pickup to show the refund policy prominently
        long daysUntilPickup = computeDaysUntilPickup(startDateStr);
        String policyMessage;
        if (daysUntilPickup > 7) {
            policyMessage = "✅ Full refund (100%) – more than 7 days before pickup.";
        } else if (daysUntilPickup >= 2) {
            policyMessage = "⚠️ Partial refund (50%) – cancellation is 2–7 days before pickup.";
        } else if (daysUntilPickup >= 0) {
            policyMessage = "❌ No refund – cancellation within 2 days of pickup is non-refundable.";
        } else {
            policyMessage = "ℹ️ Refund policy will be determined by the booking dates.";
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText("Cancel Booking " + shorten(bookingId, 16));
        confirm.setContentText("Are you sure you want to cancel this booking?\n\n"
                + "📋 Refund Policy:\n" + policyMessage);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String userId = LoginController.getUserId();
                String result = bookingService.cancelBooking(bookingId, userId);
                if (result != null) {
                    // Build a helpful status message based on the policy
                    String successMsg;
                    if (daysUntilPickup > 7) {
                        successMsg = "✅ Booking cancelled. Full refund will be processed within 3–5 business days.";
                    } else if (daysUntilPickup >= 2) {
                        successMsg = "✅ Booking cancelled. 50% partial refund will be processed within 3–5 business days.";
                    } else if (daysUntilPickup >= 0) {
                        successMsg = "✅ Booking cancelled. No refund is applicable per our policy.";
                    } else {
                        successMsg = "✅ Booking cancelled successfully.";
                    }
                    setStatus(successMsg);
                    loadBookings();
                } else {
                    setStatus("❌ Failed to cancel booking. Please try again.");
                }
            }
        });
    }

    /** Parse start-date from booking row (format: "YYYY-MM-DD" or array "[2024,4,15,...]")
     *  and compute how many days remain until pickup. Returns -1 if unparsable. */
    private long computeDaysUntilPickup(String startDateStr) {
        if (startDateStr == null || "-".equals(startDateStr)) return -1;
        try {
            // Numeric date string: "2024-04-15"
            String clean = startDateStr.trim();
            if (clean.matches("\\d{4}-\\d{2}-\\d{2}")) {
                java.time.LocalDate start = java.time.LocalDate.parse(clean);
                return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), start);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    @FXML public void filterAll() { updateTable(allBookings); resetFilterButtons(allBtn); }
    @FXML public void filterActive() {
        updateTable(filterByStatus("ACTIVE", "CONFIRMED"));
        resetFilterButtons(activeBtn);
    }
    @FXML public void filterCompleted() {
        updateTable(filterByStatus("COMPLETED"));
        resetFilterButtons(completedBtn);
    }
    @FXML public void filterCancelled() {
        updateTable(filterByStatus("CANCELLED"));
        resetFilterButtons(cancelledBtn);
    }

    private List<String[]> filterByStatus(String... statuses) {
        List<String[]> result = new ArrayList<>();
        for (String[] b : allBookings) {
            for (String s : statuses) {
                if (s.equalsIgnoreCase(b[4])) { result.add(b); break; }
            }
        }
        return result;
    }

    private void updateTable(List<String[]> data) {
        ObservableList<String[]> items = FXCollections.observableArrayList(data);
        bookingsTable.setItems(items);
        if (bookingCountLabel != null) {
            bookingCountLabel.setText(data.size() + " booking" + (data.size() != 1 ? "s" : "") + " found");
        }
    }

    private void resetFilterButtons(ToggleButton active) {
        for (ToggleButton btn : new ToggleButton[]{allBtn, activeBtn, completedBtn, cancelledBtn}) {
            if (btn != null) {
                btn.getStyleClass().removeAll("filter-tab-active", "filter-tab");
                btn.getStyleClass().add("filter-tab");
            }
        }
        if (active != null) {
            active.getStyleClass().removeAll("filter-tab", "filter-tab-active");
            active.getStyleClass().add("filter-tab-active");
        }
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

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private String extract(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        if (start >= json.length()) return null;
        char ch = json.charAt(start);
        if (ch == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        } else if (ch == 'n') {
            return null;
        } else {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return end > start ? json.substring(start, end).trim() : null;
        }
    }
}