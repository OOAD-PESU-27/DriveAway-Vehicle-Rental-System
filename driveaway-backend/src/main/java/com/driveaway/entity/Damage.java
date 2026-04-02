package com.driveaway.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Damage Entity - Represents vehicle damage records
 * GRASP: Information Expert - Handles damage-related data
 */
@Document(collection = "damages")
public class Damage {

    @Id
    private String id;

    private String vehicleId;
    private String bookingId;
    private String reportedBy; // User ID
    private String description;
    private double estimatedRepairCost;
    private String severity; // MINOR, MODERATE, SEVERE
    private String status; // REPORTED, UNDER_REPAIR, REPAIRED
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;

    public Damage() {}

    public Damage(String vehicleId, String bookingId, String reportedBy,
                  String description, double estimatedRepairCost, String severity) {
        this.vehicleId = vehicleId;
        this.bookingId = bookingId;
        this.reportedBy = reportedBy;
        this.description = description;
        this.estimatedRepairCost = estimatedRepairCost;
        this.severity = severity;
        this.status = "REPORTED";
        this.reportedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getEstimatedRepairCost() { return estimatedRepairCost; }
    public void setEstimatedRepairCost(double estimatedRepairCost) { this.estimatedRepairCost = estimatedRepairCost; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}