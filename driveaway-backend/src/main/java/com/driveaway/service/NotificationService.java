package com.driveaway.service;

import com.driveaway.entity.Notification;
import com.driveaway.entity.Payment;
import com.driveaway.NotificationType;
import com.driveaway.repository.NotificationRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * NotificationService - Contains business logic for notifications (Option B: simulated)
 * GRASP: Information Expert - Handles notification-related business logic
 * SOLID: SRP - Only handles notification operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    /**
     * Send simulated payment request notification (Option B - no real SMTP).
     * Persists a notification record with the approval token for local testing.
     */
    public void sendPaymentRequestNotification(Payment payment, String approvalToken) {
        try {
            String approveUrl = "POST /api/v1/payments/" + payment.getId() + "/approve  (or use token: " + approvalToken + ")";
            Notification notification = new Notification(
                    payment.getUserId(),
                    payment.getId(),
                    NotificationType.PAYMENT_REQUEST_SENT,
                    "Payment request of ₹" + payment.getAmount() + " has been submitted and is pending approval. " +
                    "Simulated approval action: " + approveUrl,
                    "Payment Request Submitted – Awaiting Approval"
            );
            notification.setApprovalToken(approvalToken);
            notification.setNotificationChannel("EMAIL");
            notification.setStatus("SENT");

            Notification savedNotification = notificationRepository.save(notification);

            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Payment request notification sent (simulated EMAIL)");

            // Log simulated email for local debugging
            log.info("[SIMULATED EMAIL] To user={} Subject='{}' ApprovalToken={}",
                    payment.getUserId(), notification.getSubject(), approvalToken);
            log.info("[SIMULATED EMAIL] To approve: POST /api/v1/payments/{}/approve", payment.getId());

        } catch (Exception e) {
            auditLogService.logNotificationAction("NOTIFICATION_ERROR", null, payment.getUserId(),
                    "Error sending payment request notification: " + e.getMessage());
        }
    }

    /**
     * Send payment approval notification.
     */
    public void sendPaymentApprovalNotification(Payment payment) {
        try {
            Notification notification = new Notification(
                    payment.getUserId(),
                    payment.getId(),
                    NotificationType.PAYMENT_APPROVED,
                    "Your payment request of ₹" + payment.getAmount() + " has been approved. " +
                    "You can now complete your payment at: POST /api/v1/payments/" + payment.getId() + "/complete",
                    "Payment Request Approved"
            );
            notification.setStatus("ACCEPTED");

            Notification savedNotification = notificationRepository.save(notification);

            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Payment approval notification sent");

            log.info("[SIMULATED EMAIL] To user={} Subject='{}'", payment.getUserId(), notification.getSubject());

        } catch (Exception e) {
            auditLogService.logNotificationAction("NOTIFICATION_ERROR", null, payment.getUserId(),
                    "Error sending approval notification: " + e.getMessage());
        }
    }

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

            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Payment success notification sent");

            log.info("[SIMULATED EMAIL] To user={} Subject='{}'", payment.getUserId(), notification.getSubject());
            
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

            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Payment failure notification sent");

            log.info("[SIMULATED EMAIL] To user={} Subject='{}'", payment.getUserId(), notification.getSubject());
            
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

            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Refund notification sent");

            log.info("[SIMULATED EMAIL] To user={} Subject='{}'", payment.getUserId(), notification.getSubject());
            
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
     * Get all notifications (admin)
     */
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
    
    /**
     * Mark notification as read
     */
    public void markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new PaymentException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
