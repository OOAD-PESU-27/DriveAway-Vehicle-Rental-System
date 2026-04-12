package com.driveaway.entity;

import com.driveaway.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Notification Entity - Represents user notifications
 * GRASP: Information Expert - Handles notification-related data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {
    
    @Id
    private String id;
    
    private String userId;
    private String paymentId;
    private NotificationType type; // PAYMENT_REQUEST_SENT, PAYMENT_APPROVED, PAYMENT_SUCCESS, etc.
    private String message;
    private String subject;
    private boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private String notificationChannel; // EMAIL, SMS, IN_APP
    private String referenceEntityId;   // Generic reference to related entity
    private String approvalToken;       // Simulated approval token for Option-B flow
    private String status;              // SENT, ACCEPTED, REJECTED (for approval notifications)
    
    public Notification(String userId, String paymentId, NotificationType type, String message, String subject) {
        this.userId = userId;
        this.paymentId = paymentId;
        this.type = type;
        this.message = message;
        this.subject = subject;
        this.isRead = false;
        this.sentAt = LocalDateTime.now();
        this.notificationChannel = "IN_APP";
        this.status = "SENT";
    }
}
