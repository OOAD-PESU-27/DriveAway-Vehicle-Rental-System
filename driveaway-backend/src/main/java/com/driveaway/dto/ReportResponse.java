package com.driveaway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ReportResponse DTO - Data Transfer Object for report API responses
 * GRASP: DTO Pattern - Separates API layer from internal model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    
    private String reportId;
    private String reportType;
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
    
    public ReportResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}