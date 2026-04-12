package com.driveaway.models;

public class Booking {

    private String bookingId;
    private String userId;
    private String vehicleId;

    public Booking() {}

    public Booking(String bookingId, String userId, String vehicleId) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.vehicleId = vehicleId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public String getVehicleId() {
        return vehicleId;
    }
}