package com.driveaway.controller;

import com.driveaway.entity.Report;
import com.driveaway.entity.AuditLog;
import com.driveaway.entity.Vehicle;
import com.driveaway.dto.ReportResponse;
import com.driveaway.service.ReportService;
import com.driveaway.service.AuditLogService;
import com.driveaway.service.VehicleService;
import com.driveaway.exception.PaymentException;
import com.driveaway.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AdminController - Handles admin operations, fleet management, analytics, and reports
 * GRASP: Controller Pattern - Handles system events and user requests
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin
public class AdminController {
    
    private final ReportService reportService;
    private final AuditLogService auditLogService;
    private final VehicleService vehicleService;

    // -------------------------------------------------------------------------
    // Fleet management endpoints
    // -------------------------------------------------------------------------

    /**
     * List all vehicles (admin fleet view)
     * GET /api/v1/admin/fleet
     */
    @GetMapping("/fleet")
    public ResponseEntity<?> listFleet(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            auditLogService.logPaymentAction("FLEET_VIEWED", null, adminId,
                    "Admin viewed fleet: " + vehicles.size() + " vehicles");
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add a vehicle to the fleet
     * POST /api/v1/admin/fleet
     */
    @PostMapping("/fleet")
    public ResponseEntity<?> addVehicle(
            @RequestBody Vehicle vehicle,
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            Vehicle saved = vehicleService.addVehicle(vehicle, adminId);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update a vehicle in the fleet
     * PUT /api/v1/admin/fleet/{vehicleId}
     */
    @PutMapping("/fleet/{vehicleId}")
    public ResponseEntity<?> updateVehicle(
            @PathVariable String vehicleId,
            @RequestBody Vehicle vehicle,
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            Vehicle updated = vehicleService.updateVehicle(vehicleId, vehicle, adminId);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete a vehicle from the fleet
     * DELETE /api/v1/admin/fleet/{vehicleId}
     */
    @DeleteMapping("/fleet/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(
            @PathVariable String vehicleId,
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            vehicleService.deleteVehicle(vehicleId, adminId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------------------------------------------------------------
    // Report management endpoints
    // -------------------------------------------------------------------------

    /**
     * List all generated reports
     * GET /api/v1/admin/reports
     */
    @GetMapping("/reports")
    public ResponseEntity<?> listAllReports(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            List<Report> reports = reportService.getAllReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate vehicle inventory report
     * GET /api/v1/admin/reports/vehicles
     */
    @GetMapping("/reports/vehicles")
    public ResponseEntity<?> generateVehicleReport(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            ReportResponse response = reportService.generateVehicleInventoryReport(adminId);
            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(new ReportResponse(e.getMessage(), false));
        }
    }

    /**
     * Generate daily report
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
     * Generate weekly report
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
     * Generate monthly report
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
     * Get report by ID
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
     * Get reports by type
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

    // -------------------------------------------------------------------------
    // Audit log endpoints
    // -------------------------------------------------------------------------

    /**
     * Get all audit logs
     * GET /api/v1/admin/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            return ResponseEntity.ok(auditLogService.getAllAuditLogs());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get audit logs by action
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
     * Get audit logs for user
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
     * Admin dashboard analytics
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
