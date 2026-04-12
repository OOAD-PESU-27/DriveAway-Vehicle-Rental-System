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
    RENTAL_CONFIRMATION("Rental Confirmation"),
    BOOKING_CONFIRMED("Booking Confirmed"),
    BOOKING_CANCELLED("Booking Cancelled"),
    CANCELLATION_REFUND_PROCESSED("Cancellation Refund Processed"),
    VEHICLE_RETURN_COMPLETED("Vehicle Return Completed"),
    DAMAGE_PENALTY_APPLIED("Damage Penalty Applied"),
    PAYMENT_APPROVAL_LINK_GENERATED("Payment Approval Link Generated"),
    PAYMENT_EMAIL_VERIFICATION("Payment Email Verification"),
    PAYMENT_VERIFIED("Payment Verified");
    
    private final String description;
    
    NotificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
