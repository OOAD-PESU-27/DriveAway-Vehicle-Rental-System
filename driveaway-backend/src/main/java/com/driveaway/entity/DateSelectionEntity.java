package com.driveaway.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "dates_db")
public class DateSelectionEntity {

    @Id
    private String id;

    private String vehicleId;
    private String startDate;
    private String endDate;
    private String status; // CONFIRMED, PENDING

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}