package com.driveaway;

/**
 * PaymentStatus Enum - Represents different states of a payment
 */
public enum PaymentStatus {
    REQUESTED("Requested"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    PENDING("Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    SUCCESS("Success"),        // backward-compat alias for COMPLETED
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");
    
    private final String value;
    
    PaymentStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}