package com.driveaway.controller;

import com.driveaway.entity.Report;
import com.driveaway.entity.AuditLog;
import com.driveaway.dto.ReportResponse;
import com.driveaway.service.ReportService;
import com.driveaway.service.AuditLogService;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AdminController - Handles HTTP requests related to admin operations, analytics, and reports
 * GRASP: Controller Pattern - Handles system events and user requests
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final ReportService reportService;
    private final AuditLogService auditLogService;
    
    /**
     * Generate daily report endpoint
     * GET /api/v1/admin/reports/daily
     */
    @GetMapping("/reports/daily")
    public ResponseEntity<?> generateDailyReport(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            ReportResponse response = reportService.generateDailyReport(adminId);
            return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(
                new ReportResponse(e.getMessage(), false)
            );
        }
    }
    
    /**
     * Generate weekly report endpoint
     * GET /api/v1/admin/reports/weekly
     */
    @GetMapping("/reports/weekly")
    public ResponseEntity<?> generateWeeklyReport(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            ReportResponse response = reportService.generateWeeklyReport(adminId);
            return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(
                new ReportResponse(e.getMessage(), false)
            );
        }
    }
    
    /**
     * Generate monthly report endpoint
     * GET /api/v1/admin/reports/monthly
     */
    @GetMapping("/reports/monthly")
    public ResponseEntity<?> generateMonthlyReport(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            ReportResponse response = reportService.generateMonthlyReport(adminId);
            return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(
                new ReportResponse(e.getMessage(), false)
            );
        }
    }
    
    /**
     * Get report by ID endpoint
     * GET /api/v1/admin/reports/{reportId}
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<?> getReport(@PathVariable String reportId) {
        try {
            Report report = reportService.getReportById(reportId);
            return ResponseEntity.ok(report);
        } catch (PaymentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get reports by type endpoint
     * GET /api/v1/admin/reports/type/{type}
     */
    @GetMapping("/reports/type/{type}")
    public ResponseEntity<?> getReportsByType(@PathVariable String type) {
        try {
            List<Report> reports = reportService.getReportsByType(type.toUpperCase());
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get audit logs endpoint
     * GET /api/v1/admin/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        try {
            return ResponseEntity.ok("Audit logs endpoint");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get audit logs by action endpoint
     * GET /api/v1/admin/audit-logs/action/{action}
     */
    @GetMapping("/audit-logs/action/{action}")
    public ResponseEntity<?> getAuditLogsByAction(@PathVariable String action) {
        try {
            List<AuditLog> logs = auditLogService.getAuditLogsByAction(action);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get audit logs for user endpoint
     * GET /api/v1/admin/audit-logs/user/{userId}
     */
    @GetMapping("/audit-logs/user/{userId}")
    public ResponseEntity<?> getAuditLogsForUser(@PathVariable String userId) {
        try {
            List<AuditLog> logs = auditLogService.getAuditLogsForUser(userId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Admin dashboard analytics endpoint
     * GET /api/v1/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardAnalytics(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            ReportResponse response = reportService.generateDailyReport(adminId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}