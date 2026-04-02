package com.driveaway.service;

import com.driveaway.entity.Notification;
import com.driveaway.entity.Payment;
import com.driveaway.NotificationType;
import com.driveaway.repository.NotificationRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * NotificationService - Contains business logic for notifications
 * GRASP: Information Expert - Handles notification-related business logic
 * SOLID: SRP - Only handles notification operations
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    
    /**
     * Send payment success notification (Observer Pattern)
     */
    public void sendPaymentSuccessNotification(Payment payment) {
        try {
            Notification notification = new Notification(
                payment.getUserId(),
                payment.getId(),
                NotificationType.PAYMENT_SUCCESS,
                "Your payment of ₹" + payment.getAmount() + " has been processed successfully. " +
                "Transaction ID: " + payment.getTransactionId(),
                "Payment Successful"
            );
            
            Notification savedNotification = notificationRepository.save(notification);
            
            // Log audit trail
            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Payment success notification sent");
            
            // In real scenario, send email/SMS here
            sendEmail(payment.getUserId(), notification.getSubject(), notification.getMessage());
            
        } catch (Exception e) {
            auditLogService.logNotificationAction("NOTIFICATION_ERROR", null, payment.getUserId(),
                    "Error sending notification: " + e.getMessage());
        }
    }
    
    /**
     * Send payment failure notification
     */
    public void sendPaymentFailureNotification(Payment payment) {
        try {
            Notification notification = new Notification(
                payment.getUserId(),
                payment.getId(),
                NotificationType.PAYMENT_FAILED,
                "Your payment of ₹" + payment.getAmount() + " has failed. " +
                "Reason: " + payment.getFailureReason() + ". Please try again.",
                "Payment Failed"
            );
            
            Notification savedNotification = notificationRepository.save(notification);
            
            // Log audit trail
            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Payment failure notification sent");
            
            // In real scenario, send email/SMS here
            sendEmail(payment.getUserId(), notification.getSubject(), notification.getMessage());
            
        } catch (Exception e) {
            auditLogService.logNotificationAction("NOTIFICATION_ERROR", null, payment.getUserId(),
                    "Error sending notification: " + e.getMessage());
        }
    }
    
    /**
     * Send refund notification
     */
    public void sendRefundNotification(Payment payment) {
        try {
            Notification notification = new Notification(
                payment.getUserId(),
                payment.getId(),
                NotificationType.REFUND_COMPLETED,
                "Your refund of ₹" + payment.getAmount() + " has been processed successfully. " +
                "It will be reflected in your account within 3-5 business days.",
                "Refund Processed"
            );
            
            Notification savedNotification = notificationRepository.save(notification);
            
            // Log audit trail
            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Refund notification sent");
            
            // In real scenario, send email/SMS here
            sendEmail(payment.getUserId(), notification.getSubject(), notification.getMessage());
            
        } catch (Exception e) {
            auditLogService.logNotificationAction("NOTIFICATION_ERROR", null, payment.getUserId(),
                    "Error sending notification: " + e.getMessage());
        }
    }
    
    /**
     * Get notifications for user
     */
    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }
    
    /**
     * Get unread notifications for user
     */
    public List<Notification> getUnreadNotificationsForUser(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * Mark notification as read
     */
    public void markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new PaymentException("Notification not found"));
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    /**
     * Simulated email sending
     * In real scenario, integrate with Email Service (SendGrid, AWS SES, etc.)
     */
    private void sendEmail(String userId, String subject, String message) {
        System.out.println("Email sent to user: " + userId);
        System.out.println("Subject: " + subject);
        System.out.println("Message: " + message);
    }
}