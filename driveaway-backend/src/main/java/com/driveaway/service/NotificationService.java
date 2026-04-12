package com.driveaway.service;

import com.driveaway.entity.Notification;
import com.driveaway.entity.Payment;
import com.driveaway.entity.User;
import com.driveaway.NotificationType;
import com.driveaway.repository.NotificationRepository;
import com.driveaway.repository.UserRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * NotificationService - Contains business logic for notifications.
 * Sends approval-request emails via EmailService (real SMTP when configured,
 * log-only fallback when not configured).
 * GRASP: Information Expert - Handles notification-related business logic
 * SOLID: SRP - Only handles notification operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Send a payment approval-request email to the user.
     * Uses EmailService which sends via real SMTP when configured, or logs in
     * simulation mode otherwise. Persists a notification record either way.
     */
    public void sendPaymentRequestNotification(Payment payment, String approvalToken) {
        try {
            String approveUrl = baseUrl + "/api/v1/payments/approve-by-token?token=" + approvalToken;
            Notification notification = new Notification(
                    payment.getUserId(),
                    payment.getId(),
                    NotificationType.PAYMENT_APPROVAL_LINK_GENERATED,
                    "Payment request of ₹" + payment.getAmount() + " has been submitted and is pending approval. " +
                    "Click the link to approve: " + approveUrl,
                    "Payment Request Submitted – Awaiting Your Approval"
            );
            notification.setApprovalToken(approvalToken);
            notification.setNotificationChannel("EMAIL");
            notification.setStatus("SENT");

            Notification savedNotification = notificationRepository.save(notification);

            auditLogService.logNotificationAction("NOTIFICATION_SENT", savedNotification.getId(),
                    payment.getUserId(), "Payment request approval email sent");

            // Send approval email to the user
            sendApprovalRequestEmail(payment, approvalToken, approveUrl);

        } catch (Exception e) {
            log.error("[NOTIFICATION] Error sending payment request notification for payment={}: {}",
                    payment.getId(), e.getMessage(), e);
            auditLogService.logNotificationAction("NOTIFICATION_ERROR", null, payment.getUserId(),
                    "Error sending payment request notification: " + e.getMessage());
        }
    }

    /**
     * Resolves the user's email address and sends the approval-request email.
     * When no email address is found the notification status is updated to reflect
     * that the email was not delivered.
     */
    private void sendApprovalRequestEmail(Payment payment, String approvalToken, String approveUrl) {
        try {
            Optional<User> userOpt = userRepository.findById(payment.getUserId());
            String userEmail = userOpt.map(User::getEmail).orElse(null);
            String userName  = userOpt.map(User::getName).orElse(null);

            if (userEmail == null || userEmail.isBlank()) {
                log.warn("[NOTIFICATION] No email address for userId={} – approval request email not sent. " +
                        "Approval token: {}", payment.getUserId(), approvalToken);
                // Update the persisted notification to indicate email was not sent
                notificationRepository.findAll().stream()
                        .filter(n -> payment.getId().equals(n.getPaymentId())
                                && approvalToken.equals(n.getApprovalToken()))
                        .findFirst()
                        .ifPresent(n -> {
                            n.setStatus("NOT_SENT");
                            notificationRepository.save(n);
                        });
                return;
            }

            String greeting = (userName != null && !userName.isBlank()) ? "Hello " + userName + "," : "Hello,";
            String subject = "DriveAway – Approve Your Payment Request (Booking #" + payment.getRentalId() + ")";
            String body = String.format(
                    "%s%n%n" +
                    "A payment request of ₹%.2f has been submitted for Booking #%s.%n%n" +
                    "To approve this payment and proceed with your rental, please click the link below:%n%n" +
                    "  %s%n%n" +
                    "If you did not make this request, please ignore this email or contact us immediately.%n%n" +
                    "Thank you,%n" +
                    "DriveAway Team",
                    greeting, payment.getAmount(), payment.getRentalId(), approveUrl);

            emailService.sendEmail(userEmail, subject, body);
            log.info("[NOTIFICATION] Approval request email sent to={} for payment={}", userEmail, payment.getId());
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to send approval request email for payment={}: {}",
                    payment.getId(), e.getMessage(), e);
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
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public long markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        if (unread.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setRead(true);
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    public List<Notification> getPendingApprovalNotificationsForUser(String userId) {
        return notificationRepository.findByUserIdAndTypeInAndIsReadFalse(
                userId,
                Set.of(NotificationType.PAYMENT_APPROVAL_LINK_GENERATED, NotificationType.PAYMENT_REQUEST_SENT));
    }

    public void sendBookingConfirmedNotification(com.driveaway.entity.Booking booking) {
        Notification notification = new Notification(
                booking.getUserId(),
                null,
                NotificationType.BOOKING_CONFIRMED,
                "Booking " + booking.getId() + " is confirmed for vehicle " + booking.getVehicleId() + ".",
                "Booking Confirmed"
        );
        notification.setReferenceEntityId(booking.getId());
        notificationRepository.save(notification);
    }

    public void sendVehicleReturnCompletedNotification(com.driveaway.entity.Booking booking) {
        Notification notification = new Notification(
                booking.getUserId(),
                null,
                NotificationType.VEHICLE_RETURN_COMPLETED,
                "Vehicle return completed for booking " + booking.getId() + ".",
                "Vehicle Return Completed"
        );
        notification.setReferenceEntityId(booking.getId());
        notificationRepository.save(notification);
    }

    public void sendDamagePenaltyAppliedNotification(com.driveaway.entity.Booking booking) {
        Notification notification = new Notification(
                booking.getUserId(),
                null,
                NotificationType.DAMAGE_PENALTY_APPLIED,
                "Damage penalty of ₹" + booking.getDamageCharge() + " applied for booking " + booking.getId() + ".",
                "Damage Penalty Applied"
        );
        notification.setReferenceEntityId(booking.getId());
        notificationRepository.save(notification);
    }
}
