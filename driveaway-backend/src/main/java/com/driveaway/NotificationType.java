package com.driveaway;

/**
 * NotificationType Enum - Represents different types of notifications
 */
public enum NotificationType {
    PAYMENT_REQUEST_SENT("Payment Request Sent"),
    PAYMENT_APPROVED("Payment Approved"),
    PAYMENT_SUCCESS("Payment Successful"),
    PAYMENT_FAILED("Payment Failed"),
    PAYMENT_COMPLETED("Payment Completed"),
    REFUND_INITIATED("Refund Initiated"),
    REFUND_COMPLETED("Refund Completed"),
    RENTAL_CONFIRMATION("Rental Confirmation");
    
    private final String description;
    
    NotificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}