package com.driveaway.controllers;

import com.driveaway.services.PaymentService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * PaymentController (Frontend) - Handles payment page interactions.
 * Implements the Option-B approval-gate workflow:
 *   1) User submits payment request → status REQUESTED, notification logged
 *   2) Admin/simulated approval → status APPROVED
 *   3) User completes payment → status COMPLETED/SUCCESS
 */
public class PaymentController {

    @FXML private TextField rentalIdField;
    @FXML private TextField amountField;
    @FXML private TextField securityDepositField;
    @FXML private ComboBox<String> paymentMethodBox;
    @FXML private TextField cardNumberField;
    @FXML private TextField upiIdField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private TextField cardHolderField;
    @FXML private ComboBox<String> bankCombo;
    // Netbanking account detail fields
    @FXML private TextField netBankingAccountHolderField;
    @FXML private TextField netBankingAccountNumberField;
    @FXML private TextField netBankingIfscField;
    @FXML private VBox cardSection;
    @FXML private VBox upiSection;
    @FXML private VBox netBankingSection;
    @FXML private Label statusLabel;
    @FXML private Label summaryAmountLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label paymentIdLabel;
    @FXML private Label approvalTokenLabel;
    @FXML private VBox approvalSection;

    private final PaymentService paymentService = new PaymentService();
    private String currentPaymentId;
    private String currentApprovalToken;

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
        // Pre-fill amount from booking's totalPrice
        double totalPrice = BookingManagementController.getLastBookingTotalPrice();
        if (totalPrice > 0 && amountField != null) {
            amountField.setText(String.format("%.2f", totalPrice));
            updateSummary();
        }
        showCardSection();
        if (approvalSection != null) {
            approvalSection.setVisible(false);
            approvalSection.setManaged(false);
        }
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
            if (summaryAmountLabel != null) summaryAmountLabel.setText("\u20b9" + String.format("%.2f", amount));
            if (totalAmountLabel != null) totalAmountLabel.setText("\u20b9" + String.format("%.2f", amount));
        } catch (NumberFormatException e) {
            if (summaryAmountLabel != null) summaryAmountLabel.setText("\u20b90.00");
            if (totalAmountLabel != null) totalAmountLabel.setText("\u20b90.00");
        }
    }

    /**
     * Step 1: Initiate payment request (Option-B approval-gate flow).
     * Creates payment in REQUESTED state + triggers in-app approval notification.
     */
    @FXML
    public void handleRequestPayment() {
        String userId = LoginController.getUserId();
        if (userId == null) { setStatus("Please log in first.", false); return; }

        String rentalId = rentalIdField != null ? rentalIdField.getText().trim() : "";
        String amount = amountField != null ? amountField.getText().trim() : "";
        String method = paymentMethodBox != null ? paymentMethodBox.getValue() : "CARD";
        String deposit = securityDepositField != null ? securityDepositField.getText().trim() : "0";

        if (rentalId.isBlank()) { setStatus("Booking ID is required.", false); return; }
        if (amount.isBlank()) { setStatus("Amount is required.", false); return; }

        // Validate netbanking fields
        if ("NETBANKING".equals(method)) {
            String bank = bankCombo != null ? bankCombo.getValue() : null;
            String acHolder = netBankingAccountHolderField != null ? netBankingAccountHolderField.getText().trim() : "";
            String acNumber = netBankingAccountNumberField != null ? netBankingAccountNumberField.getText().trim() : "";
            String ifsc = netBankingIfscField != null ? netBankingIfscField.getText().trim() : "";
            if (bank == null || bank.isBlank()) { setStatus("Please select a bank for NetBanking.", false); return; }
            if (acHolder.isBlank()) { setStatus("Account holder name is required for NetBanking.", false); return; }
            if (acNumber.isBlank()) { setStatus("Account number is required for NetBanking.", false); return; }
            if (ifsc.isBlank()) { setStatus("IFSC code is required for NetBanking.", false); return; }
        }

        double parsedAmount;
        try {
            parsedAmount = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            setStatus("Invalid amount entered.", false);
            return;
        }

        double parsedDeposit = 0;
        try {
            if (!deposit.isBlank()) parsedDeposit = Double.parseDouble(deposit);
        } catch (NumberFormatException ignored) {}

        setStatus("⏳ Submitting payment request...", false);
        String response = paymentService.initiatePaymentRequest(
                rentalId, userId, parsedAmount, method, parsedDeposit);

        if (response != null && response.contains("\"success\":true")) {
            currentPaymentId = extractField(response, "paymentId");
            currentApprovalToken = extractField(response, "approvalToken");
            if (currentPaymentId == null) currentPaymentId = extractField(response, "id");

            String displayId = currentPaymentId != null ? currentPaymentId : "N/A";
            setStatus("✅ Payment request submitted! Use the in-app approval button below to approve the payment.", true);
            if (paymentIdLabel != null) paymentIdLabel.setText("Payment ID: " + displayId);

            // Show approval section after request is submitted
            if (approvalSection != null) {
                approvalSection.setVisible(true);
                approvalSection.setManaged(true);
            }
            if (approvalTokenLabel != null) {
                approvalTokenLabel.setText("Payment ID: " + displayId + "\nClick \"Approve Payment (In-App)\" to approve, then complete the payment.");
            }
        } else {
            setStatus("❌ Failed to submit payment request. Please try again.", false);
        }
    }

    /**
     * In-app approval: approves the payment directly in the app without email.
     * This replaces the email-link approval dependency.
     */
    @FXML
    public void handleApproveInApp() {
        if (currentPaymentId == null) {
            setStatus("No pending payment found. Please submit a payment request first.", false);
            return;
        }
        String userId = LoginController.getUserId();
        String approvedBy = userId != null ? userId : "IN_APP_USER";
        setStatus("⏳ Approving payment...", false);
        String response = paymentService.approvePayment(currentPaymentId, approvedBy);
        if (response != null && response.contains("\"success\":true")) {
            setStatus("✅ Payment approved in-app! Click \"Complete Payment\" to finalise your booking.", true);
            if (approvalTokenLabel != null) {
                approvalTokenLabel.setText("✅ Approved! Click \"Complete Payment\" below to finalise your booking.");
            }
        } else {
            String msg = response != null ? extractField(response, "message") : null;
            setStatus("❌ " + (msg != null ? msg : "Approval failed. Please try again."), false);
        }
    }

    /**
     * Checks the current approval status of the submitted payment request.
     * If approved, the user can proceed to complete payment.
     */
    @FXML
    public void handleCheckApprovalStatus() {
        if (currentPaymentId == null) {
            setStatus("No pending payment found. Please submit a payment request first.", false);
            return;
        }
        setStatus("⏳ Checking approval status...", false);
        String response = paymentService.getPaymentById(currentPaymentId);
        if (response != null) {
            String status = extractField(response, "status");
            if ("APPROVED".equals(status)) {
                setStatus("✅ Payment has been approved! You can now complete the payment.", true);
                if (approvalTokenLabel != null) {
                    approvalTokenLabel.setText("✅ Approved! Click \"Complete Payment\" below to finalise your booking.");
                }
            } else if ("REQUESTED".equals(status) || "PENDING_APPROVAL".equals(status)) {
                setStatus("⏳ Approval is still pending. Click \"Approve Payment (In-App)\" above to approve.", false);
            } else if ("COMPLETED".equals(status) || "SUCCESS".equals(status)) {
                setStatus("✅ Payment already completed.", true);
            } else {
                setStatus("Current status: " + (status != null ? status : "unknown") + ". Please contact support if this is unexpected.", false);
            }
        } else {
            setStatus("❌ Could not retrieve payment status. Please try again.", false);
        }
    }

    /**
     * Step 3: Complete the payment (only succeeds after approval).
     */
    @FXML
    public void handleCompletePayment() {
        String userId = LoginController.getUserId();
        if (userId == null) { setStatus("Please log in first.", false); return; }
        if (currentPaymentId == null) {
            setStatus("No approved payment found. Please request and approve first.", false);
            return;
        }
        setStatus("⏳ Processing payment...", false);
        String response = paymentService.completePayment(currentPaymentId, userId);
        if (response != null && response.contains("\"success\":true")) {
            setStatus("✅ Payment completed successfully! Your booking is confirmed.", true);
            if (approvalSection != null) {
                approvalSection.setVisible(false);
                approvalSection.setManaged(false);
            }
            currentPaymentId = null;
            currentApprovalToken = null;
        } else {
            String msg = response != null ? extractField(response, "message") : null;
            setStatus("❌ " + (msg != null ? msg : "Payment failed. Please try again."), false);
        }
    }

    /**
     * Legacy: direct pay (kept for backward compat, skips approval gate).
     */
    @FXML
    public void handlePay() {
        String userId = LoginController.getUserId();
        if (userId == null) { setStatus("Please log in first.", false); return; }

        String rentalId = rentalIdField != null ? rentalIdField.getText() : "";
        String amount = amountField != null ? amountField.getText() : "";
        String method = paymentMethodBox != null ? paymentMethodBox.getValue() : "CARD";

        if (rentalId.isBlank()) { setStatus("Booking ID is required.", false); return; }
        if (amount.isBlank()) { setStatus("Amount is required.", false); return; }

        double parsedAmount;
        try {
            parsedAmount = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            setStatus("Invalid amount entered.", false);
            return;
        }

        String cardNumber = cardNumberField != null ? cardNumberField.getText() : "";
        setStatus("Processing payment...", false);
        String response = paymentService.processPayment(rentalId, userId, parsedAmount, method, cardNumber);

        if (response != null && response.contains("\"success\":true")) {
            setStatus("✅ Payment successful! Your booking is confirmed.", true);
        } else {
            setStatus("❌ Payment failed. Please check your details and try again.", false);
        }
    }

    // Navigation
    @FXML public void goToDashboard() { SceneNavigator.load("views/DashboardView.fxml"); }
    @FXML public void goToVehicles() { SceneNavigator.load("views/VehicleCatalogView.fxml"); }
    @FXML public void goToBookings() { SceneNavigator.load("views/BookingManagementView.fxml"); }
    @FXML public void goToProfile() { SceneNavigator.load("views/UserProfileView.fxml"); }
    @FXML public void goToNotifications() { SceneNavigator.load("views/NotificationsView.fxml"); }
    @FXML public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }

    private void setStatus(String message, boolean isSuccess) {
        if (statusLabel != null) {
            statusLabel.getStyleClass().removeAll("text-success", "text-danger", "text-muted");
            if (isSuccess) statusLabel.getStyleClass().add("text-success");
            else if (message.startsWith("\u274c") || message.startsWith("❌")) {
                statusLabel.getStyleClass().add("text-danger");
            } else {
                statusLabel.getStyleClass().add("text-muted");
            }
            statusLabel.setText(message);
        }
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
        return null;
    }
}
