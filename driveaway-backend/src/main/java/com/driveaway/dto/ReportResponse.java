package com.driveaway.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private String reportId;
    private String reportType;
    private String title;
    private LocalDateTime reportDate;

    private long totalTransactions;
    private double totalRevenue;
    private double successfulTransactions;
    private double failedTransactions;
    private double refundedAmount;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime generatedAt;
    private String generatedBy;

    private String message;
    private boolean success;

    private long totalVehicles;
    private long availableVehicles;
    private long bookedVehicles;
    private long maintenanceVehicles;

    private double totalSecurityDeposit;
    private double releasedSecurityDeposit;

    private String mostUsedVehicleId;
    private long totalBookings;
    private double maintenanceCost;
    private long damageIncidents;
    private double totalDamageCharges;

    public ReportResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}