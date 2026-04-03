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

    public static void setLastBookingId(String id) {
        lastBookingId = id;
    }

    public static String getLastBookingId() {
        return lastBookingId;
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
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                "₹" + data.getValue()[5]));

        // Action column with Cancel button
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel");
            {
                cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
                cancelBtn.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    handleCancel(row[0], row[4]);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    String[] row = getTableView().getItems().get(getIndex());
                    String status = row[4];
                    boolean canCancel = "ACTIVE".equalsIgnoreCase(status)
                            || "CONFIRMED".equalsIgnoreCase(status)
                            || "PENDING".equalsIgnoreCase(status);
                    cancelBtn.setDisable(!canCancel);
                    setGraphic(cancelBtn);
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
            String amount = extract(entry, "totalAmount");

            if (id != null) {
                allBookings.add(new String[]{
                        id,
                        vehicleId != null ? vehicleId : "-",
                        start != null ? start : "-",
                        end != null ? end : "-",
                        status != null ? status : "PENDING",
                        amount != null ? amount : "0"
                });
            }
        }

        setStatus("");
        updateTable(allBookings);
    }

    private void handleCancel(String bookingId, String status) {
        if (!"ACTIVE".equalsIgnoreCase(status) && !"CONFIRMED".equalsIgnoreCase(status)
                && !"PENDING".equalsIgnoreCase(status)) {
            setStatus("This booking cannot be cancelled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText("Cancel Booking");
        confirm.setContentText("Are you sure you want to cancel booking " + shorten(bookingId, 16) + "?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String userId = LoginController.getUserId();
                String result = bookingService.cancelBooking(bookingId, userId);
                if (result != null) {
                    setStatus("✅ Booking cancelled successfully.");
                    loadBookings();
                } else {
                    setStatus("❌ Failed to cancel booking. Please try again.");
                }
            }
        });
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
        String defaultStyle = "-fx-background-color: #f1f5f9; -fx-text-fill: #374151; -fx-background-radius: 6; -fx-padding: 6 16 6 16; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #1e40af; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 16 6 16; -fx-cursor: hand;";
        if (allBtn != null) allBtn.setStyle(defaultStyle);
        if (activeBtn != null) activeBtn.setStyle(defaultStyle);
        if (completedBtn != null) completedBtn.setStyle(defaultStyle);
        if (cancelledBtn != null) cancelledBtn.setStyle(defaultStyle);
        if (active != null) active.setStyle(activeStyle);
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
