package com.driveaway.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Report Entity - Represents analytics and revenue reports
 * GRASP: Information Expert - Handles report-related data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reports")
public class Report {
    
    @Id
    private String id;
    
    private String reportType; // DAILY, WEEKLY, MONTHLY, CUSTOM, VEHICLE_INVENTORY
    private LocalDateTime reportDate;
    private long totalTransactions;
    private double totalRevenue;
    private double successfulTransactions;
    private double failedTransactions;
    private double refundedAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime generatedAt;
    private String generatedBy; // Admin ID
    private String title;

    // Vehicle inventory report fields
    private long totalVehicles;
    private long availableVehicles;
    private long bookedVehicles;
    private long maintenanceVehicles;

    // Security deposit summary
    private double totalSecurityDeposit;
    private double releasedSecurityDeposit;

    // Extended analytics fields
    private String mostUsedVehicleId;
    private long totalBookings;
    private double maintenanceCost;
    private long damageIncidents;
    private double totalDamageCharges;
    
    public Report(String reportType, LocalDateTime startDate, LocalDateTime endDate) {
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reportDate = LocalDateTime.now();
        this.generatedAt = LocalDateTime.now();
        this.totalTransactions = 0;
        this.totalRevenue = 0.0;
    }
}
