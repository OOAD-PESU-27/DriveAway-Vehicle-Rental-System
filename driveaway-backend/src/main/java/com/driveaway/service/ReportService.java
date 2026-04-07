package com.driveaway.service;

import com.driveaway.entity.Report;
import com.driveaway.entity.Payment;
import com.driveaway.entity.Vehicle;
import com.driveaway.PaymentStatus;
import com.driveaway.dto.ReportResponse;
import com.driveaway.repository.ReportRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
    private final VehicleRepository vehicleRepository;
    private final AuditLogService auditLogService;

    /**
     * Generate vehicle inventory/availability report
     */
    public ReportResponse generateVehicleInventoryReport(String adminId) {
        try {
            List<Vehicle> allVehicles = vehicleRepository.findAll();
            long total = allVehicles.size();
            long available = allVehicles.stream().filter(Vehicle::isAvailable).count();
            long booked = allVehicles.stream()
                    .filter(v -> "BOOKED".equalsIgnoreCase(v.getStatus())).count();
            long maintenance = allVehicles.stream()
                    .filter(v -> "MAINTENANCE".equalsIgnoreCase(v.getStatus())).count();

            Report report = new Report("VEHICLE_INVENTORY", LocalDateTime.now(), LocalDateTime.now());
            report.setTitle("Vehicle Inventory & Availability Report");
            report.setGeneratedBy(adminId);
            report.setTotalVehicles(total);
            report.setAvailableVehicles(available);
            report.setBookedVehicles(booked);
            report.setMaintenanceVehicles(maintenance);

            Report savedReport = reportRepository.save(report);

            auditLogService.logReportAction("REPORT_GENERATED", savedReport.getId(), adminId,
                    "Vehicle inventory report generated: total=" + total + " available=" + available);

            ReportResponse response = new ReportResponse("Vehicle inventory report generated successfully", true);
            response.setReportId(savedReport.getId());
            response.setReportType(savedReport.getReportType());
            response.setTitle(savedReport.getTitle());
            response.setReportDate(savedReport.getReportDate());
            response.setGeneratedAt(savedReport.getGeneratedAt());
            response.setGeneratedBy(adminId);
            response.setTotalVehicles(total);
            response.setAvailableVehicles(available);
            response.setBookedVehicles(booked);
            response.setMaintenanceVehicles(maintenance);
            return response;
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_ERROR", null, adminId,
                    "Error generating vehicle inventory report: " + e.getMessage());
            throw new PaymentException("Error generating vehicle inventory report: " + e.getMessage());
        }
    }
    
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
            List<Payment> payments = paymentRepository.findByPaymentDateBetween(startDate, endDate);
            
            Report report = new Report(reportType, startDate, endDate);
            report.setTitle(reportType + " Payment Transactions Report");
            report.setGeneratedBy(adminId);
            
            long totalTransactions = payments.size();
            double totalRevenue = 0.0;
            double successfulCount = 0;
            double failedCount = 0;
            double refundedAmount = 0.0;
            double totalSecurityDeposit = 0.0;
            double releasedSecurityDeposit = 0.0;
            
            for (Payment payment : payments) {
                if (Set.of(PaymentStatus.SUCCESS, PaymentStatus.COMPLETED).contains(payment.getStatus())) {
                    totalRevenue += payment.getAmount();
                    successfulCount++;
                } else if (payment.getStatus() == PaymentStatus.FAILED) {
                    failedCount++;
                } else if (payment.getStatus() == PaymentStatus.REFUNDED) {
                    refundedAmount += payment.getAmount();
                }
                totalSecurityDeposit += payment.getSecurityDeposit();
                if ("RELEASED".equals(payment.getSecurityDepositStatus())) {
                    releasedSecurityDeposit += payment.getSecurityDeposit();
                }
            }
            
            report.setTotalTransactions(totalTransactions);
            report.setTotalRevenue(totalRevenue);
            report.setSuccessfulTransactions(successfulCount);
            report.setFailedTransactions(failedCount);
            report.setRefundedAmount(refundedAmount);
            report.setTotalSecurityDeposit(totalSecurityDeposit);
            report.setReleasedSecurityDeposit(releasedSecurityDeposit);
            
            Report savedReport = reportRepository.save(report);
            
            auditLogService.logReportAction("REPORT_GENERATED", savedReport.getId(), adminId,
                    "Report generated for period: " + startDate + " to " + endDate);
            
            ReportResponse response = new ReportResponse(
                savedReport.getId(),
                savedReport.getReportType(),
                savedReport.getTitle(),
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
                true,
                0L, 0L, 0L, 0L,
                totalSecurityDeposit,
                releasedSecurityDeposit
            );
            return response;
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
            // Reports are generated on-demand; this hook is available for future aggregation
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
            // Reports are generated on-demand
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

    /**
     * Get all reports
     */
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }
}
