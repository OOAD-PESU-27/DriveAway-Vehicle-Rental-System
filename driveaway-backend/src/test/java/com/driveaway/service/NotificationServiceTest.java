package com.driveaway.service;

import com.driveaway.NotificationType;
import com.driveaway.PaymentStatus;
import com.driveaway.entity.Notification;
import com.driveaway.entity.Payment;
import com.driveaway.entity.User;
import com.driveaway.repository.NotificationRepository;
import com.driveaway.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private EmailService emailService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Payment buildPayment(String userId, double amount, PaymentStatus status) {
        Payment p = new Payment("rental-1", userId, amount, "CARD");
        p.setId("pay-test-001");
        p.setStatus(status);
        p.setTransactionId("TXN_TEST001");
        p.setFailureReason("Declined");
        return p;
    }

    @Test
    void sendPaymentRequestNotification_persists_notification_with_token() {
        Payment payment = buildPayment("user-1", 2000.0, PaymentStatus.REQUESTED);
        String approvalToken = "APPR_TESTTOKEN123";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId("notif-001");
            return n;
        });
        // User lookup for email – return empty so email sending is skipped gracefully
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        notificationService.sendPaymentRequestNotification(payment, approvalToken);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals("pay-test-001", saved.getPaymentId());
        assertEquals(NotificationType.PAYMENT_REQUEST_SENT, saved.getType());
        assertEquals(approvalToken, saved.getApprovalToken());
        assertEquals("EMAIL", saved.getNotificationChannel());
        assertEquals("SENT", saved.getStatus());
        assertFalse(saved.isRead());
    }

    @Test
    void sendPaymentRequestNotification_withUserEmail_sendsApprovalEmail() {
        Payment payment = buildPayment("user-5", 3000.0, PaymentStatus.REQUESTED);
        String approvalToken = "APPR_EMAILTOKEN";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId("notif-005");
            return n;
        });

        User user = new User();
        user.setId("user-5");
        user.setEmail("customer@example.com");
        user.setName("Test Customer");
        when(userRepository.findById("user-5")).thenReturn(Optional.of(user));

        notificationService.sendPaymentRequestNotification(payment, approvalToken);

        // Verify email was sent with correct recipient and token in URL
        ArgumentCaptor<String> toCaptor    = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor  = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(toCaptor.capture(), anyString(), bodyCaptor.capture());

        assertEquals("customer@example.com", toCaptor.getValue());
        assertTrue(bodyCaptor.getValue().contains(approvalToken),
                "Email body must contain the approval token");
        assertTrue(bodyCaptor.getValue().contains("/approve-by-token"),
                "Email body must contain the approval link path");
    }

    @Test
    void sendPaymentApprovalNotification_persists_accepted_notification() {
        Payment payment = buildPayment("user-2", 1500.0, PaymentStatus.APPROVED);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId("notif-002");
            return n;
        });

        notificationService.sendPaymentApprovalNotification(payment);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.PAYMENT_APPROVED, saved.getType());
        assertEquals("ACCEPTED", saved.getStatus());
    }

    @Test
    void sendPaymentSuccessNotification_persists_success_notification() {
        Payment payment = buildPayment("user-3", 3000.0, PaymentStatus.SUCCESS);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId("notif-003");
            return n;
        });

        notificationService.sendPaymentSuccessNotification(payment);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.PAYMENT_SUCCESS, saved.getType());
        assertTrue(saved.getMessage().contains("TXN_TEST001"));
    }

    @Test
    void sendPaymentFailureNotification_persists_failure_notification() {
        Payment payment = buildPayment("user-4", 1000.0, PaymentStatus.FAILED);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.sendPaymentFailureNotification(payment);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        assertEquals(NotificationType.PAYMENT_FAILED, captor.getValue().getType());
    }

    @Test
    void markAsRead_updatesNotification() {
        Notification notification = new Notification("u1", "p1", NotificationType.PAYMENT_SUCCESS, "msg", "subj");
        notification.setId("notif-read");

        when(notificationRepository.findById("notif-read")).thenReturn(java.util.Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead("notif-read");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().isRead());
    }
}
