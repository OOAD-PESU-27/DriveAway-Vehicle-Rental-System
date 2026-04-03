package com.driveaway.controllers;

import com.driveaway.utils.HttpUtil;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class PaymentController {

    private static final String BASE_URL = "http://localhost:8080";

    @FXML private TextField rentalIdField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> paymentMethodBox;
    @FXML private TextField cardNumberField;
    @FXML private TextField upiIdField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private TextField cardHolderField;
    @FXML private ComboBox<String> bankCombo;
    @FXML private VBox cardSection;
    @FXML private VBox upiSection;
    @FXML private VBox netBankingSection;
    @FXML private Label statusLabel;
    @FXML private Label summaryAmountLabel;
    @FXML private Label totalAmountLabel;

    @FXML
    public void initialize() {
        if (paymentMethodBox != null) {
            paymentMethodBox.getItems().addAll("CARD", "UPI", "NETBANKING");
            paymentMethodBox.setValue("CARD");
        }
        if (bankCombo != null) {
            bankCombo.getItems().addAll("SBI", "HDFC", "ICICI", "Axis Bank",
                    "Kotak", "PNB", "Bank of Baroda");
        }
        // Pre-fill booking ID if available
        String bookingId = BookingManagementController.getLastBookingId();
        if (bookingId != null && rentalIdField != null) {
            rentalIdField.setText(bookingId);
        }
        showCardSection();
    }

    @FXML
    public void handleMethodChange() {
        String method = paymentMethodBox != null ? paymentMethodBox.getValue() : "CARD";
        if ("CARD".equals(method)) showCardSection();
        else if ("UPI".equals(method)) showUpiSection();
        else showNetBankingSection();
    }

    private void showCardSection() {
        setVisible(cardSection, true);
        setVisible(upiSection, false);
        setVisible(netBankingSection, false);
    }

    private void showUpiSection() {
        setVisible(cardSection, false);
        setVisible(upiSection, true);
        setVisible(netBankingSection, false);
    }

    private void showNetBankingSection() {
        setVisible(cardSection, false);
        setVisible(upiSection, false);
        setVisible(netBankingSection, true);
    }

    private void setVisible(VBox section, boolean visible) {
        if (section != null) {
            section.setVisible(visible);
            section.setManaged(visible);
        }
    }

    @FXML
    public void updateSummary() {
        String amountText = amountField != null ? amountField.getText() : "";
        try {
            double amount = Double.parseDouble(amountText);
            if (summaryAmountLabel != null) summaryAmountLabel.setText("₹" + String.format("%.2f", amount));
            if (totalAmountLabel != null) totalAmountLabel.setText("₹" + String.format("%.2f", amount));
        } catch (NumberFormatException e) {
            if (summaryAmountLabel != null) summaryAmountLabel.setText("₹0.00");
            if (totalAmountLabel != null) totalAmountLabel.setText("₹0.00");
        }
    }

    @FXML
    public void handlePay() {
        String userId = LoginController.getUserId();
        if (userId == null) { setStatus("Please log in first."); return; }

        String rentalId = rentalIdField != null ? rentalIdField.getText() : "";
        String amount = amountField != null ? amountField.getText() : "";
        String method = paymentMethodBox != null ? paymentMethodBox.getValue() : "CARD";

        if (rentalId.isBlank()) { setStatus("Booking ID is required."); return; }
        if (amount.isBlank()) { setStatus("Amount is required."); return; }

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
            String bank = bankCombo != null && bankCombo.getValue() != null
                    ? bankCombo.getValue() : "SBI";
            json = String.format(
                    "{\"rentalId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,"
                            + "\"paymentMethod\":\"NETBANKING\",\"netBankingBank\":\"%s\"}",
                    rentalId, userId, parsedAmount, bank);
        }

        setStatus("Processing payment...");
        String response = HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/payments/process", json, "X-User-ID", userId);

        if (response != null && response.contains("\"success\":true")) {
            setStatus("✅ Payment successful! Your booking is confirmed.");
        } else {
            setStatus("❌ Payment failed. Please check your details and try again.");
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

    private void setStatus(String message) {
        if (statusLabel != null) {
            boolean isSuccess = message.startsWith("✅");
            boolean isError = message.startsWith("❌");
            statusLabel.getStyleClass().removeAll("text-success", "text-danger", "text-muted");
            if (isSuccess) statusLabel.getStyleClass().add("text-success");
            else if (isError) statusLabel.getStyleClass().add("text-danger");
            else statusLabel.getStyleClass().add("text-muted");
            statusLabel.setText(message);
        }
    }
}

