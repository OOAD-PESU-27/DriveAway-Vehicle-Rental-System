package com.driveaway.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import com.driveaway.services.VehicleService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VehicleController {
    @FXML
    private FlowPane vehicleContainer;

    @FXML
    private DatePicker startDate;

    @FXML
    private DatePicker endDate;

    private VehicleService service = new VehicleService();

    @FXML
    public void initialize() {
        // Optional: can automatically load vehicles, or leave blank
    }

    @FXML
    private void handleSearch() {
        if (startDate.getValue() == null || endDate.getValue() == null) {
            showAlert("Validation Error", "Please select both a start and end date.");
            return;
        }
        String start = startDate.getValue().toString();
        String end = endDate.getValue().toString();

        String response = service.getVehiclesWithPricing(start, end);
        if (response == null || response.trim().isEmpty()) {
            showAlert("Error", "No response from server.");
            return;
        }

        List<VehicleData> vehicles = parseVehicles(response);
        vehicleContainer.getChildren().clear();

        for (VehicleData v : vehicles) {
            VBox card = createVehicleCard(v);
            vehicleContainer.getChildren().add(card);
        }
    }

    private List<VehicleData> parseVehicles(String json) {
        List<VehicleData> vehicles = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String obj = matcher.group();
            VehicleData vehicle = parseVehicle(obj);
            if (vehicle.id != null) {
                vehicles.add(vehicle);
            }
        }
        return vehicles;
    }

    private VehicleData parseVehicle(String json) {
        VehicleData v = new VehicleData();
        v.id = getJsonString(json, "id");
        v.name = getJsonString(json, "name");
        v.seatingCapacity = getJsonInt(json, "seatingCapacity");
        v.pricePerDay = getJsonDouble(json, "pricePerDay");
        v.weekendPricePerDay = getJsonDouble(json, "weekendPricePerDay");
        v.holidayPricePerDay = getJsonDouble(json, "holidayPricePerDay");
        v.totalPrice = String.valueOf(getJsonDouble(json, "totalPrice"));
        v.priceBreakdown = getJsonString(json, "priceBreakdown");
        return v;
    }

    private String getJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private int getJsonInt(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private double getJsonDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.]+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0.0;
    }

    private VBox createVehicleCard(VehicleData v) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-pref-width: 240;");

        VBox body = new VBox(8);

        Label title = new Label(v.name);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label seats = new Label("Seats: " + v.seatingCapacity);
        seats.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");

        Label baseLabel = new Label("Base: ₹" + v.pricePerDay);
        baseLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");

        Label weekendLabel = new Label("Weekend: ₹" + v.weekendPricePerDay);
        weekendLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");

        Label holidayLabel = new Label("Holiday: ₹" + v.holidayPricePerDay);
        holidayLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");

        Label totalLabel = new Label("₹" + v.totalPrice);
        totalLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2b6cb0; -fx-padding: 10 0 10 0;");

        Button bookBtn = new Button("Book Now");
        bookBtn.setStyle("-fx-background-color: #2b6cb0; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 6; -fx-cursor: hand;");

        bookBtn.setOnAction(e -> {
            String breakdown = v.priceBreakdown.isEmpty() ? "No details available" : v.priceBreakdown;
            breakdown = breakdown.replace("\\n", "\n");
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("Price Breakdown");

            VBox container = new VBox(10);
            container.setStyle("-fx-padding: 15; -fx-background-color: white;");

            Label breakdownTitle = new Label("Calculation Details");
            breakdownTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a202c; -fx-padding: 0 0 10 0;");

            String[] lines = breakdown.split("\n");
            VBox breakdownBox = new VBox(6);

            for (String line : lines) {
                Label l = new Label(line);
                l.setStyle("-fx-font-size: 14px; -fx-text-fill: #718096;");
                breakdownBox.getChildren().add(l);
            }

            container.getChildren().addAll(breakdownTitle, breakdownBox);

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setContent(container);
            dialogPane.setStyle("-fx-background-color: white;");

            ButtonType okButton = new ButtonType("Book", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(okButton, cancelButton);

            alert.showAndWait().ifPresent(response -> {
                if (response == okButton) {
                    bookVehicle(v.id);
                }
            });
        });

        body.getChildren().addAll(title, seats, baseLabel, weekendLabel, holidayLabel, totalLabel, bookBtn);
        card.getChildren().add(body);
        return card;
    }

    private void bookVehicle(String vehicleId) {
        // If you have login functionality, fetch userId accordingly. Here it's just a placeholder string.
        String currentUserId = "demoUser"; // Replace with actual logic if you have authentication

        String json = "{" +
                "\"userId\":\"" + currentUserId + "\"," +
                "\"vehicleId\":\"" + vehicleId + "\"," +
                "\"startDate\":\"" + startDate.getValue().toString() + "\"," +
                "\"endDate\":\"" + endDate.getValue().toString() + "\"" +
                "}";

        service.bookVehicle(json);
        showAlert("Booking", "Booking sent for User ID: " + currentUserId);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    static class VehicleData {
        String id;
        String name;
        int seatingCapacity;
        double pricePerDay;
        double weekendPricePerDay;
        double holidayPricePerDay;
        String totalPrice;
        String priceBreakdown;
    }
}