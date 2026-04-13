package com.driveaway.dto;

/**
 * InspectionRequest — DTO sent from JavaFX frontend when staff submits vehicle return inspection.
 *
 * Fields match EXACTLY what BookingController.java (frontend) sends:
 *   {
 *     "bookingId": "...",
 *     "vehicleId": "...",
 *     "hasDamage": true/false,
 *     "damageDescription": "Scratched bumper",
 *     "damageSeverity": "MEDIUM"
 *   }
 */
public class InspectionRequest {

    private String bookingId;
    private String vehicleId;
    private boolean hasDamage;
    private String damageDescription;
    private String damageSeverity;

    public InspectionRequest() {}

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public boolean isHasDamage() { return hasDamage; }
    public void setHasDamage(boolean hasDamage) { this.hasDamage = hasDamage; }

    public String getDamageDescription() { return damageDescription; }
    public void setDamageDescription(String damageDescription) { this.damageDescription = damageDescription; }

    public String getDamageSeverity() { return damageSeverity; }
    public void setDamageSeverity(String damageSeverity) { this.damageSeverity = damageSeverity; }
}
