package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class PaymentService {

    private static final String BASE_URL = "http://localhost:8080";

    /**
     * Step 1: Initiate a payment request (REQUESTED state, triggers notification).
     */
    public String initiatePaymentRequest(String rentalId, String userId, double amount,
                                         String paymentMethod, double securityDeposit) {
        String json = String.format(
                "{\"rentalId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f,"
                + "\"paymentMethod\":\"%s\",\"securityDeposit\":%.2f}",
                rentalId, userId, amount, paymentMethod, securityDeposit);
        return HttpUtil.sendPostWithHeader(BASE_URL + "/api/v1/payments/request", json, "X-User-ID", userId);
    }

    /**
     * Step 2: Approve a payment (simulated admin/recipient acceptance).
     */
    public String approvePayment(String paymentId, String approvedBy) {
        return HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/payments/" + paymentId + "/approve",
                "{}",
                "X-Approved-By", approvedBy);
    }

    /**
     * Approve via simulated token (Option B: mimics clicking email link).
     */
    public String approveByToken(String approvalToken) {
        return HttpUtil.sendPost(
                BASE_URL + "/api/v1/payments/approve-by-token?token=" + approvalToken, "{}");
    }

    /**
     * Step 3: Complete the payment (only succeeds if APPROVED).
     */
    public String completePayment(String paymentId, String userId) {
        return HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/payments/" + paymentId + "/complete",
                "{}",
                "X-User-ID", userId);
    }

    /**
     * Get payment status by ID.
     */
    public String getPaymentById(String paymentId) {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/payments/" + paymentId);
    }

    /**
     * Get all payments for a user.
     */
    public String getUserPayments(String userId) {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/payments/user/" + userId);
    }

    /**
     * Legacy direct process (kept for backward compat).
     */
    public String processPayment(String rentalId, String userId, double amount,
                                  String paymentMethod, String cardNumber) {
        String json = String.format(
                "{\"rentalId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f,"
                + "\"paymentMethod\":\"%s\",\"cardNumber\":\"%s\"}",
                rentalId, userId, amount, paymentMethod, cardNumber);
        return HttpUtil.sendPostWithHeader(
                BASE_URL + "/api/v1/payments/process", json, "X-User-ID", userId);
    }
}
