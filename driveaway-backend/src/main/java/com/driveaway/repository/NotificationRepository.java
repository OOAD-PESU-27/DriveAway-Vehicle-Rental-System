package com.driveaway.repository;

import com.driveaway.entity.Notification;
import com.driveaway.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationRepository - Handles database operations for notifications
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    List<Notification> findByUserId(String userId);
    
    List<Notification> findByPaymentId(String paymentId);
    
    List<Notification> findByType(NotificationType type);
    
    List<Notification> findByUserIdAndIsReadFalse(String userId);

    List<Notification> findByUserIdAndTypeAndIsReadFalse(String userId, NotificationType type);
    
    long countByUserIdAndIsReadFalse(String userId);
    
    List<Notification> findBySentAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
