package com.driveaway.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * AuditLog Entity - Represents system audit trail (NFR: Auditability)
 * GRASP: Information Expert - Handles audit-related data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {
    
    @Id
    private String id;
    
    private String action; // PAYMENT_INITIATED, PAYMENT_SUCCESS, PAYMENT_FAILED, REPORT_GENERATED, NOTIFICATION_SENT
    private String entityType; // PAYMENT, REPORT, NOTIFICATION, USER
    private String entityId;
    private String performedBy; // User/Admin ID
    private String details; // Additional context
    private LocalDateTime timestamp;
    private String ipAddress;
    private String status; // SUCCESS, FAILURE
    
    public AuditLog(String action, String entityType, String entityId, String performedBy, String details) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.performedBy = performedBy;
        this.details = details;
        this.timestamp = LocalDateTime.now();
        this.status = "SUCCESS";
    }
}