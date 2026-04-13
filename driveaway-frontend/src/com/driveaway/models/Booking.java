package com.driveaway.models;

/**
 * Booking (Frontend Model) — mirrors the backend Booking entity JSON.
 * Used for parsing JSON responses from backend.
 *
 * Fields match backend entity:
 *   id, userId, vehicleId, startDate, endDate,
 *   totalPrice, status, licenseVerified, createdAt
 */
public class Booking {

    private String id;
    private String userId;
    private String vehicleId;
    private String startDate;
    private String endDate;
    private double totalPrice;
    private String status;
    private boolean licenseVerified;
    private String createdAt;

    public Booking() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isLicenseVerified() { return licenseVerified; }
    public void setLicenseVerified(boolean licenseVerified) { this.licenseVerified = licenseVerified; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Booking[" + id + " | " + vehicleId + " | " + status + " | ₹" + totalPrice + "]";
    }
}