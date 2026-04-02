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
    
    private String reportType; // DAILY, WEEKLY, MONTHLY, CUSTOM
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
