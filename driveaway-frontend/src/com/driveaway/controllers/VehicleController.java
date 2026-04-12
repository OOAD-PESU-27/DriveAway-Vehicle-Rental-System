package com.driveaway.controllers;


import javafx.scene.control.Label;
import com.driveaway.services.VehicleService;
import javafx.fxml.FXML;
// ✅ CORRECT
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import org.json.JSONArray;
import org.json.JSONObject;

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
        
    }

    @FXML
    private void handleSearch() {

        String start = startDate.getValue().toString();
        String end = endDate.getValue().toString();

        String response = service.getVehiclesWithPricing(start, end);

        JSONArray vehicles = new JSONArray(response);

        // ✅ clear previous results
        vehicleContainer.getChildren().clear();

        for (int i = 0; i < vehicles.length(); i++) {

            JSONObject v = vehicles.getJSONObject(i);

            VBox card = createVehicleCard(v);

            vehicleContainer.getChildren().add(card);
        }
    }

    private VBox createVehicleCard(JSONObject v) {

        VBox card = new VBox();
        card.getStyleClass().add("vehicle-card");

        VBox body = new VBox(6);
        body.getStyleClass().add("vehicle-card-body");

        Label title = new Label(v.optString("name"));
        title.getStyleClass().add("vehicle-name");

        Label seats = new Label("Seats: " + v.optInt("seatingCapacity"));
        seats.getStyleClass().add("spec-label");

        double base = v.optDouble("pricePerDay");
        double weekend = v.optDouble("weekendPricePerDay");
        double holiday = v.optDouble("holidayPricePerDay");
        double total = Double.parseDouble(v.optString("totalPrice", "0"));

        Label baseLabel = new Label("Base: ₹" + base);
        baseLabel.getStyleClass().add("vehicle-price-label");

        Label weekendLabel = new Label("Weekend: ₹" + weekend);
        weekendLabel.getStyleClass().add("vehicle-price-label");

        Label holidayLabel = new Label("Holiday: ₹" + holiday);
        holidayLabel.getStyleClass().add("vehicle-price-label");

        Label totalLabel = new Label("₹" + total);
        totalLabel.getStyleClass().add("vehicle-price");

        totalLabel.setStyle("-fx-cursor: hand;");

        totalLabel.setOnMouseClicked(e -> {

        String breakdown = v.optString("priceBreakdown", "No details available");

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Price Breakdown");

        // 🔥 Create layout
        VBox container = new VBox(10);
        container.setStyle("-fx-padding: 15;");

        Label breakdownTitle  = new Label("Calculation Details");
        breakdownTitle .getStyleClass().add("section-title");

        // Split breakdown lines
        String[] lines = breakdown.split("\n");

        VBox breakdownBox = new VBox(6);

        for (String line : lines) {
            Label l = new Label(line);
            l.getStyleClass().add("text-muted");
            breakdownBox.getChildren().add(l);
        }

        container.getChildren().addAll(breakdownTitle , breakdownBox);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(container);

        // ✅ Apply your CSS
        dialogPane.getStylesheets().add(
            new java.io.File("src/com/driveaway/views/style/main.css")
                .toURI().toString()
        );

        dialogPane.getStyleClass().add("card");

        alert.getButtonTypes().setAll(new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));

        alert.showAndWait();
    });

        Button bookBtn = new Button("Book Now");
        bookBtn.getStyleClass().addAll("btn-primary", "btn-small");

        bookBtn.setOnAction(e -> bookVehicle(v.optString("id")));

        body.getChildren().addAll(
            title,
            seats,
            baseLabel,
            weekendLabel,
            holidayLabel,
            totalLabel,
            bookBtn
        );

        card.getChildren().add(body);

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