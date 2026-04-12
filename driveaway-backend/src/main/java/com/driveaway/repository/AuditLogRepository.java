package com.driveaway.repository;

import com.driveaway.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AuditLogRepository - Handles database operations for audit logs
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    
    List<AuditLog> findByAction(String action);
    
    List<AuditLog> findByEntityId(String entityId);
    
    List<AuditLog> findByPerformedBy(String performedBy);
    
    List<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<AuditLog> findByStatus(String status);
}