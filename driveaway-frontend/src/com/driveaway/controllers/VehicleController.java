package com.driveaway.controllers;

import javafx.scene.control.Label;
import com.driveaway.services.VehicleService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import javafx.scene.control.ListView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class VehicleController {

    @FXML
    private ListView<VBox> vehicleList;

    @FXML
    private DatePicker startDate;

    @FXML
    private DatePicker endDate;

    private VehicleService service = new VehicleService();

    @FXML
    public void initialize() {
        vehicleList.setVisible(false);
    }

    public void searchVehicles() {

        if (startDate.getValue() == null || endDate.getValue() == null) {
            System.out.println("Select dates");
            return;
        }

        vehicleList.setVisible(true);

        String start = startDate.getValue().toString();
        String end = endDate.getValue().toString();

        String response = service.getVehiclesWithPricing(start, end);

        JSONArray vehicles = new JSONArray(response);

        vehicleList.getItems().clear();

        for (int i = 0; i < vehicles.length(); i++) {
            JSONObject v = vehicles.getJSONObject(i);
            vehicleList.getItems().add(createVehicleCard(v));
        }
    }

    private VBox createVehicleCard(JSONObject v) {

        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-border-radius: 10; -fx-border-color: #ddd;");

        // Title
        Label title = new Label("🚗 " + v.optString("name", "Vehicle"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Seating
        Label seats = new Label("Seats: " + v.optInt("seatingCapacity", 0));

        // Prices
        double base = v.optDouble("pricePerDay", 0);
        double weekend = v.optDouble("weekendPricePerDay", 0);
        double holiday = v.optDouble("holidayPricePerDay", 0);
        double total = v.optDouble("totalPrice", 0);

        Label baseLabel = new Label("Base: ₹" + base + "/day");
        Label weekendLabel = new Label("Weekend: ₹" + weekend + "/day");
        Label holidayLabel = new Label("Holiday: ₹" + holiday + "/day");

        Label totalLabel = new Label("Total: ₹" + total);
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: green;");

        // Button
        Button bookBtn = new Button("Book");
        bookBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        bookBtn.setOnAction(e -> bookVehicle(v.optString("id")));

        // Add all to card
        card.getChildren().addAll(
            title,
            seats,
            baseLabel,
            weekendLabel,
            holidayLabel,
            totalLabel,
            bookBtn
        );

        return card;
    }

       private void bookVehicle(String vehicleId) {

        JSONObject data = new JSONObject();
        data.put("vehicleId", vehicleId);
        data.put("startDate", startDate.getValue().toString());
        data.put("endDate", endDate.getValue().toString());

        service.bookVehicle(data.toString());

        System.out.println("Booking sent");
    }
}