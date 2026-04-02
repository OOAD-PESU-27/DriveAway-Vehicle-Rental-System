package com.driveaway.controllers;

import com.driveaway.utils.HttpUtil;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class PaymentController {

    private static final String BASE_URL = "http://localhost:8080";

    @FXML
    private TextField rentalIdField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> paymentMethodBox;

    @FXML
    private TextField cardNumberField;

    @FXML
    private TextField upiIdField;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        if (paymentMethodBox != null) {
            paymentMethodBox.getItems().addAll("CARD", "UPI", "NETBANKING");
            paymentMethodBox.setValue("CARD");
        }
    }

    @FXML
    public void handlePay() {
        String userId = LoginController.getUserId();
        if (userId == null) {
            setStatus("Please log in first.");
            return;
        }

        String rentalId = rentalIdField != null ? rentalIdField.getText() : "";
        String amount = amountField != null ? amountField.getText() : "";
        String method = paymentMethodBox != null ? paymentMethodBox.getValue() : "CARD";

        if (rentalId.isBlank() || amount.isBlank()) {
            setStatus("Rental ID and amount are required.");
            return;
        }

        double parsedAmount;
        try {
            parsedAmount = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            setStatus("Invalid amount entered.");
            return;
        }

        String json;
        if ("CARD".equalsIgnoreCase(method)) {
            String cardNumber = cardNumberField != null ? cardNumberField.getText() : "";
            json = String.format(
                    "{\"rentalId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,"
                            + "\"paymentMethod\":\"CARD\",\"cardNumber\":\"%s\"}",
                    rentalId, userId, parsedAmount, cardNumber);
        } else if ("UPI".equalsIgnoreCase(method)) {
            String upiId = upiIdField != null ? upiIdField.getText() : "";
            json = String.format(
                    "{\"rentalId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,"
                            + "\"paymentMethod\":\"UPI\",\"upiId\":\"%s\"}",
                    rentalId, userId, parsedAmount, upiId);
        } else {
            json = String.format(
                    "{\"rentalId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,"
                            + "\"paymentMethod\":\"NETBANKING\",\"netBankingBank\":\"DEFAULT\"}",
                    rentalId, userId, parsedAmount);
        }

        setStatus("Processing payment...");
        String response = HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/payments/process", json, "X-User-ID", userId);

        if (response != null && response.contains("\"success\":true")) {
            setStatus("Payment successful!");
        } else {
            setStatus("Payment failed. Please try again.");
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
