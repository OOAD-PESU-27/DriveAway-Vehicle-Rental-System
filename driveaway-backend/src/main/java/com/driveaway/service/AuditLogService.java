package com.driveaway.service;

import com.driveaway.entity.AuditLog;
import com.driveaway.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * AuditLogService - Contains business logic for audit logging (NFR: Auditability)
 * GRASP: Information Expert - Handles audit-related business logic
 * SOLID: SRP - Only handles audit operations
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * Log payment action
     */
    public void logPaymentAction(String action, String paymentId, String userId, String details) {
        AuditLog auditLog = new AuditLog(
            action,
            "PAYMENT",
            paymentId,
            userId,
            details
        );
        auditLogRepository.save(auditLog);
    }
    
    /**
     * Log report action
     */
    public void logReportAction(String action, String reportId, String adminId, String details) {
        AuditLog auditLog = new AuditLog(
            action,
            "REPORT",
            reportId,
            adminId,
            details
        );
        auditLogRepository.save(auditLog);
    }
    
    /**
     * Log notification action
     */
    public void logNotificationAction(String action, String notificationId, String userId, String details) {
        AuditLog auditLog = new AuditLog(
            action,
            "NOTIFICATION",
            notificationId,
            userId,
            details
        );
        auditLogRepository.save(auditLog);
    }
    
    /**
     * Log admin/fleet action
     */
    public void logAdminAction(String action, String entityId, String adminId, String details) {
        AuditLog auditLog = new AuditLog(
            action,
            "ADMIN",
            entityId,
            adminId,
            details
        );
        auditLogRepository.save(auditLog);
    }

    /**
     * Log user/profile action
     */
    public void logUserAction(String action, String userId, String performedBy, String details) {
        AuditLog auditLog = new AuditLog(
            action,
            "USER",
            userId,
            performedBy,
            details
        );
        auditLogRepository.save(auditLog);
    }
    
    /**
     * Get all audit logs
     */
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    /**
     * Get audit logs for entity
     */
    public List<AuditLog> getAuditLogsForEntity(String entityId) {
        return auditLogRepository.findByEntityId(entityId);
    }
    
    /**
     * Get audit logs by action
     */
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }
    
    /**
     * Get audit logs performed by user
     */
    public List<AuditLog> getAuditLogsForUser(String userId) {
        return auditLogRepository.findByPerformedBy(userId);
    }
}