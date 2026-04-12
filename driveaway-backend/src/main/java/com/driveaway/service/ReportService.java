package com.driveaway.service;

import com.driveaway.entity.Report;
import com.driveaway.entity.Payment;
import com.driveaway.entity.Vehicle;
import com.driveaway.entity.Booking;
import com.driveaway.entity.MaintenanceRecord;
import com.driveaway.PaymentStatus;
import com.driveaway.dto.ReportResponse;
import com.driveaway.repository.ReportRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.MaintenanceRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final BookingRepository bookingRepository;
    private final MaintenanceRepository maintenanceRepository;

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
                releasedSecurityDeposit,
                null, 0L, 0.0, 0L, 0.0
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
     * Generate revenue report – aggregates all completed/successful payments.
     */
    public ReportResponse generateRevenueReport(String adminId) {
        try {
            List<Payment> allPayments = paymentRepository.findAll();
            double totalRevenue = 0.0;
            double refundedAmount = 0.0;
            long successfulCount = 0;
            long failedCount = 0;
            long refundedCount = 0;
            for (Payment p : allPayments) {
                if (Set.of(PaymentStatus.SUCCESS, PaymentStatus.COMPLETED).contains(p.getStatus())) {
                    totalRevenue += p.getAmount();
                    successfulCount++;
                } else if (p.getStatus() == PaymentStatus.FAILED) {
                    failedCount++;
                } else if (p.getStatus() == PaymentStatus.REFUNDED) {
                    refundedAmount += p.getAmount();
                    refundedCount++;
                }
            }
            long totalBookings = bookingRepository.count();

            Report report = new Report("REVENUE", LocalDateTime.now(), LocalDateTime.now());
            report.setTitle("Revenue Report – All-Time Summary");
            report.setGeneratedBy(adminId);
            report.setTotalTransactions(allPayments.size());
            report.setTotalRevenue(totalRevenue);
            report.setSuccessfulTransactions(successfulCount);
            report.setFailedTransactions(failedCount);
            report.setRefundedAmount(refundedAmount);
            report.setTotalBookings(totalBookings);

            Report saved = reportRepository.save(report);
            auditLogService.logReportAction("REPORT_GENERATED", saved.getId(), adminId,
                    "Revenue report generated: totalRevenue=" + totalRevenue);

            ReportResponse resp = new ReportResponse(saved.getId(), "REVENUE", saved.getTitle(),
                    saved.getReportDate(), allPayments.size(), totalRevenue, successfulCount,
                    failedCount, refundedAmount, null, null, LocalDateTime.now(), adminId,
                    "Revenue report generated successfully", true,
                    0L, 0L, 0L, 0L, 0.0, 0.0, null, 0L, 0.0, 0L, 0.0);
            resp.setTotalBookings(totalBookings);
            return resp;
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_ERROR", null, adminId,
                    "Error generating revenue report: " + e.getMessage());
            throw new PaymentException("Error generating revenue report: " + e.getMessage());
        }
    }

    /**
     * Generate vehicle usage report – shows most used vehicle and booking counts per vehicle.
     */
    public ReportResponse generateVehicleUsageReport(String adminId) {
        try {
            List<Booking> bookings = bookingRepository.findAll();
            long totalBookings = bookings.size();

            // Find most-used vehicle by booking frequency
            Map<String, Long> usageMap = bookings.stream()
                    .filter(b -> b.getVehicleId() != null)
                    .collect(Collectors.groupingBy(Booking::getVehicleId, Collectors.counting()));

            String mostUsedVehicleId = usageMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");

            long completedBookings = bookings.stream()
                    .filter(b -> "COMPLETED".equals(b.getStatus()) || "RETURNED".equals(b.getStatus()))
                    .count();
            long cancelledBookings = bookings.stream()
                    .filter(b -> "CANCELLED".equals(b.getStatus()))
                    .count();

            Report report = new Report("VEHICLE_USAGE", LocalDateTime.now(), LocalDateTime.now());
            report.setTitle("Vehicle Usage Report – Fleet Utilisation");
            report.setGeneratedBy(adminId);
            report.setTotalTransactions(totalBookings);
            report.setTotalBookings(totalBookings);
            report.setMostUsedVehicleId(mostUsedVehicleId);
            report.setSuccessfulTransactions(completedBookings);
            report.setFailedTransactions(cancelledBookings);

            Report saved = reportRepository.save(report);
            auditLogService.logReportAction("REPORT_GENERATED", saved.getId(), adminId,
                    "Vehicle usage report generated: totalBookings=" + totalBookings
                    + " mostUsed=" + mostUsedVehicleId);

            ReportResponse resp = new ReportResponse(saved.getId(), "VEHICLE_USAGE", saved.getTitle(),
                    saved.getReportDate(), totalBookings, 0.0, completedBookings, cancelledBookings,
                    0.0, null, null, LocalDateTime.now(), adminId,
                    "Vehicle usage report generated successfully", true,
                    0L, 0L, 0L, 0L, 0.0, 0.0, null, 0L, 0.0, 0L, 0.0);
            resp.setTotalBookings(totalBookings);
            resp.setMostUsedVehicleId(mostUsedVehicleId);
            return resp;
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_ERROR", null, adminId,
                    "Error generating vehicle usage report: " + e.getMessage());
            throw new PaymentException("Error generating vehicle usage report: " + e.getMessage());
        }
    }

    /**
     * Generate damage report – collects bookings that had damage charges applied.
     */
    public ReportResponse generateDamageReport(String adminId) {
        try {
            List<Booking> allBookings = bookingRepository.findAll();
            List<Booking> damagedBookings = allBookings.stream()
                    .filter(b -> b.getDamageCharge() > 0)
                    .collect(Collectors.toList());

            long damageIncidents = damagedBookings.size();
            double totalDamageCharges = damagedBookings.stream()
                    .mapToDouble(Booking::getDamageCharge)
                    .sum();

            Report report = new Report("DAMAGE", LocalDateTime.now(), LocalDateTime.now());
            report.setTitle("Damage Report – Incidents & Penalty Summary");
            report.setGeneratedBy(adminId);
            report.setTotalTransactions(damageIncidents);
            report.setDamageIncidents(damageIncidents);
            report.setTotalDamageCharges(totalDamageCharges);
            report.setTotalRevenue(totalDamageCharges);

            Report saved = reportRepository.save(report);
            auditLogService.logReportAction("REPORT_GENERATED", saved.getId(), adminId,
                    "Damage report generated: incidents=" + damageIncidents
                    + " totalCharges=" + totalDamageCharges);

            ReportResponse resp = new ReportResponse(saved.getId(), "DAMAGE", saved.getTitle(),
                    saved.getReportDate(), damageIncidents, totalDamageCharges, damageIncidents,
                    0.0, 0.0, null, null, LocalDateTime.now(), adminId,
                    "Damage report generated successfully", true,
                    0L, 0L, 0L, 0L, 0.0, 0.0, null, 0L, 0.0, 0L, 0.0);
            resp.setDamageIncidents(damageIncidents);
            resp.setTotalDamageCharges(totalDamageCharges);
            return resp;
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_ERROR", null, adminId,
                    "Error generating damage report: " + e.getMessage());
            throw new PaymentException("Error generating damage report: " + e.getMessage());
        }
    }

    /**
     * Generate maintenance report – aggregates cost and counts from maintenance records.
     */
    public ReportResponse generateMaintenanceReport(String adminId) {
        try {
            List<MaintenanceRecord> records = maintenanceRepository.findAll();
            double totalCost = records.stream().mapToDouble(MaintenanceRecord::getCost).sum();
            long completedCount = records.stream()
                    .filter(r -> "COMPLETED".equals(r.getStatus()))
                    .count();
            long scheduledCount = records.stream()
                    .filter(r -> "SCHEDULED".equals(r.getStatus()))
                    .count();

            Report report = new Report("MAINTENANCE", LocalDateTime.now(), LocalDateTime.now());
            report.setTitle("Maintenance Report – Fleet Service Summary");
            report.setGeneratedBy(adminId);
            report.setTotalTransactions(records.size());
            report.setMaintenanceCost(totalCost);
            report.setSuccessfulTransactions(completedCount);
            report.setFailedTransactions(scheduledCount);
            report.setTotalRevenue(totalCost);

            Report saved = reportRepository.save(report);
            auditLogService.logReportAction("REPORT_GENERATED", saved.getId(), adminId,
                    "Maintenance report generated: records=" + records.size()
                    + " totalCost=" + totalCost);

            ReportResponse resp = new ReportResponse(saved.getId(), "MAINTENANCE", saved.getTitle(),
                    saved.getReportDate(), records.size(), totalCost, completedCount,
                    scheduledCount, 0.0, null, null, LocalDateTime.now(), adminId,
                    "Maintenance report generated successfully", true,
                    0L, 0L, 0L, 0L, 0.0, 0.0, null, 0L, 0.0, 0L, 0.0);
            resp.setMaintenanceCost(totalCost);
            return resp;
        } catch (Exception e) {
            auditLogService.logReportAction("REPORT_ERROR", null, adminId,
                    "Error generating maintenance report: " + e.getMessage());
            throw new PaymentException("Error generating maintenance report: " + e.getMessage());
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
