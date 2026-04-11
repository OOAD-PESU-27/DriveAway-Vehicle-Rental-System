package com.driveaway.controllers;

import com.driveaway.services.VehicleService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;

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

        String response = service.getAvailableVehicles(start, end);

        JSONArray vehicles = new JSONArray(response);

        vehicleList.getItems().clear();

        for (int i = 0; i < vehicles.length(); i++) {
            JSONObject v = vehicles.getJSONObject(i);
            vehicleList.getItems().add(createVehicleCard(v));
        }
    }

    private VBox createVehicleCard(JSONObject v) {

        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-radius: 10;");

        Label title = new Label("🚗 " + v.getString("brand") + " " + v.getString("model"));
        Label type = new Label("Type: " + v.getString("vehicleType"));
        Label seats = new Label("Seats: " + v.getInt("seatingCapacity"));

        double price = v.getDouble("pricePerDay");
        Label priceLabel = new Label("₹" + price + "/day");

        long days = ChronoUnit.DAYS.between(startDate.getValue(), endDate.getValue()) + 1;
        double total = days * price;

        Label totalLabel = new Label("Total: ₹" + total);

        Button bookBtn = new Button("Book");

        bookBtn.setOnAction(e -> bookVehicle(v.getString("id")));

        card.getChildren().addAll(title, type, seats, priceLabel, totalLabel, bookBtn);

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