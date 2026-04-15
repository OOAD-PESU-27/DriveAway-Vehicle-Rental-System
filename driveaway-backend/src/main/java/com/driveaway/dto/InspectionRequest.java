package com.driveaway.dto;

/**
 * InspectionRequest DTO
 *
 * Received from Person 3's JavaFX frontend (StaffController) when staff
 * submits the return inspection form.
 *
 * The frontend sends JSON like:
 * {
 *   "bookingId":  "69dc...",
 *   "vehicleId":  "69d4...",
 *   "hasDamage":  true,
 *   "damageDescription": "Scratched rear bumper",
 *   "damageSeverity": "MEDIUM"
 * }
 *
 * WHERE TO PUT THIS FILE:
 *   driveaway-backend/src/main/java/com/driveaway/dto/InspectionRequest.java
 */
public class InspectionRequest {

    private String bookingId;
    private String vehicleId;
    private boolean hasDamage;
    private String damageDescription;
    private String damageSeverity; // LOW, MEDIUM, HIGH

    public InspectionRequest() {}

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public boolean isHasDamage() { return hasDamage; }
    public void setHasDamage(boolean hasDamage) { this.hasDamage = hasDamage; }

    public String getDamageDescription() { return damageDescription; }
    public void setDamageDescription(String damageDescription) {
        this.damageDescription = damageDescription;
    }

    public String getDamageSeverity() { return damageSeverity; }
    public void setDamageSeverity(String damageSeverity) {
        this.damageSeverity = damageSeverity;
    }
}
