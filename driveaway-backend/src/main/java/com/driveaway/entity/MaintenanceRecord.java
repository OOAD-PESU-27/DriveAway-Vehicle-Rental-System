package com.driveaway.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MaintenanceRecord Entity - Represents vehicle maintenance history
 * GRASP: Information Expert - Handles maintenance-related data
 */
@Document(collection = "maintenance_records")
public class MaintenanceRecord {

    @Id
    private String id;

    private String vehicleId;
    private String performedBy; // Staff/Admin ID
    private String maintenanceType; // OIL_CHANGE, TIRE_ROTATION, BRAKE_CHECK, FULL_SERVICE, REPAIR
    private String description;
    private double cost;
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private LocalDateTime createdAt;

    public MaintenanceRecord() {}

    public MaintenanceRecord(String vehicleId, String performedBy, String maintenanceType,
                             String description, double cost, LocalDate scheduledDate) {
        this.vehicleId = vehicleId;
        this.performedBy = performedBy;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.cost = cost;
        this.scheduledDate = scheduledDate;
        this.status = "SCHEDULED";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}