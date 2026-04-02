package com.driveaway.service;

import com.driveaway.entity.Report;
import com.driveaway.entity.Payment;
import com.driveaway.PaymentStatus;
import com.driveaway.dto.ReportResponse;
import com.driveaway.repository.ReportRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ReportService - Contains business logic for report generation and analytics
 * GRASP: Information Expert - Handles report-related business logic
 * SOLID: SRP - Only handles report operations
 */
@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogService auditLogService;
    
    /**
     * Generate daily report
     */
    public ReportResponse generateDailyReport(String adminId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59);
        
        return generateCustomReport("DAILY", startOfDay, endOfDay, adminId);
    }
    
    /**
     * Generate weekly report
     */
    public ReportResponse generateWeeklyReport(String adminId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1)
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = startOfWeek.plusDays(7)
                .withHour(23).withMinute(59).withSecond(59);
        
        return generateCustomReport("WEEKLY", startOfWeek, endOfWeek, adminId);
    }
    
    /**
     * Generate monthly report
     */
    public ReportResponse generateMonthlyReport(String adminId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.plusMonths(1).withDayOfMonth(1)
                .minusSeconds(1);
        
        return generateCustomReport("MONTHLY", startOfMonth, endOfMonth, adminId);
    }
    
    /**
     * Generate custom report for date range
     */
    public ReportResponse generateCustomReport(String reportType, LocalDateTime startDate,
                                               LocalDateTime endDate, String adminId) {
        try {
            // Fetch payments in date range
            List<Payment> payments = paymentRepository.findByPaymentDateBetween(startDate, endDate);
            
            // Create report entity
            Report report = new Report(reportType, startDate, endDate);
            report.setGeneratedBy(adminId);
            
            // Calculate metrics
            long totalTransactions = payments.size();
            double totalRevenue = 0.0;
            double successfulCount = 0;
            double failedCount = 0;
            double refundedAmount = 0.0;
            
            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    totalRevenue += payment.getAmount();
                    successfulCount++;
                } else if (payment.getStatus() == PaymentStatus.FAILED) {
                    failedCount++;
                } else if (payment.getStatus() == PaymentStatus.REFUNDED) {
                    refundedAmount += payment.getAmount();
                }
            }
            
            report.setTotalTransactions(totalTransactions);
            report.setTotalRevenue(totalRevenue);
            report.setSuccessfulTransactions(successfulCount);
            report.setFailedTransactions(failedCount);
            report.setRefundedAmount(refundedAmount);
            
            // Save report
            Report savedReport = reportRepository.save(report);
            
            // Log audit trail
            auditLogService.logReportAction("REPORT_GENERATED", savedReport.getId(), adminId,
                    "Report generated for period: " + startDate + " to " + endDate);
            
            return new ReportResponse(
                savedReport.getId(),
                savedReport.getReportType(),
                savedReport.getReportDate(),
                savedReport.getTotalTransactions(),
                savedReport.getTotalRevenue(),
                savedReport.getSuccessfulTransactions(),
                savedReport.getFailedTransactions(),
                savedReport.getRefundedAmount(),
                startDate,
                endDate,
                LocalDateTime.now(),
                adminId,
                "Report generated successfully",
                true
            );
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_ERROR", null, adminId,
                    "Error generating report: " + e.getMessage());
            throw new PaymentException("Error generating report: " + e.getMessage());
        }
    }
    
    /**
     * Update report when payment succeeds (Observer Pattern)
     */
    public void updateReportOnPaymentSuccess(Payment payment) {
        try {
            LocalDateTime paymentDate = payment.getPaymentDate();
            LocalDateTime startOfDay = paymentDate.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfDay = paymentDate.withHour(23).withMinute(59).withSecond(59);
            
            // This is where we would update daily aggregates in real scenario
            // For now, reports are generated on-demand
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_UPDATE_ERROR", null, "SYSTEM",
                    "Error updating report on payment: " + e.getMessage());
        }
    }
    
    /**
     * Update report when payment is refunded
     */
    public void updateReportOnPaymentRefund(Payment payment) {
        try {
            // Update refund metrics in real scenario
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_UPDATE_ERROR", null, "SYSTEM",
                    "Error updating report on refund: " + e.getMessage());
        }
    }
    
    /**
     * Get report by ID
     */
    public Report getReportById(String reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new PaymentException("Report not found with ID: " + reportId));
    }
    
    /**
     * Get all reports of specific type
     */
    public List<Report> getReportsByType(String reportType) {
        return reportRepository.findByReportType(reportType);
    }
}