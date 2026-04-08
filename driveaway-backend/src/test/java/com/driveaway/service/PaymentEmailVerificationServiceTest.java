package com.driveaway.service;

import com.driveaway.PaymentStatus;
import com.driveaway.entity.Notification;
import com.driveaway.entity.Payment;
import com.driveaway.entity.VerificationToken;
import com.driveaway.exception.PaymentException;
import com.driveaway.repository.NotificationRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentEmailVerificationService.
 * Covers: send, verify success, expired token, used token, resend.
 */
@ExtendWith(MockitoExtension.class)
class PaymentEmailVerificationServiceTest {

    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private PaymentEmailVerificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
    }

    // ─── sendVerificationEmail ────────────────────────────────────────────────

    @Test
    void sendVerificationEmail_generatesAndPersistsToken() {
        Payment payment = buildPayment("pay-001", PaymentStatus.COMPLETED);

        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.sendVerificationEmail(payment, "user@example.com");

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        VerificationToken saved = tokenCaptor.getValue();
        assertNotNull(saved.getToken());
        assertEquals("pay-001", saved.getPaymentId());
        assertEquals("user@example.com", saved.getUserEmail());
        assertFalse(saved.isUsed());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));

        verify(emailService).sendEmail(eq("user@example.com"), contains("Verify"), anyString());
        verify(paymentRepository).save(argThat(p -> p.isEmailVerificationSent()));
    }

    @Test
    void sendVerificationEmail_skipsSendWhenAlreadyVerified() {
        Payment payment = buildPayment("pay-002", PaymentStatus.COMPLETED);
        payment.setEmailVerified(true);

        service.sendVerificationEmail(payment, "user@example.com");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void sendVerificationEmail_skipsSendWhenEmailIsNull() {
        Payment payment = buildPayment("pay-003", PaymentStatus.COMPLETED);

        service.sendVerificationEmail(payment, null);

        verifyNoInteractions(tokenRepository, emailService);
    }

    // ─── verifyToken ─────────────────────────────────────────────────────────

    @Test
    void verifyToken_success_marksPaymentVerified() {
        VerificationToken vt = buildToken("tok-valid", "pay-010", false,
                LocalDateTime.now().plusMinutes(10));

        Payment payment = buildPayment("pay-010", PaymentStatus.COMPLETED);

        when(tokenRepository.findByToken("tok-valid")).thenReturn(Optional.of(vt));
        when(paymentRepository.findById("pay-010")).thenReturn(Optional.of(payment));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.verifyToken("tok-valid");

        assertTrue(vt.isUsed());
        verify(paymentRepository).save(argThat(p -> p.isEmailVerified()));
        verify(auditLogService).logPaymentAction(eq("PAYMENT_EMAIL_VERIFIED"), eq("pay-010"), any(), any());
    }

    @Test
    void verifyToken_invalidToken_throwsPaymentException() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> service.verifyToken("bad-token"));
        assertTrue(ex.getMessage().contains("Invalid"));
    }

    @Test
    void verifyToken_expiredToken_throwsPaymentException() {
        VerificationToken vt = buildToken("tok-expired", "pay-011", false,
                LocalDateTime.now().minusMinutes(1)); // already expired

        when(tokenRepository.findByToken("tok-expired")).thenReturn(Optional.of(vt));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> service.verifyToken("tok-expired"));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void verifyToken_usedToken_throwsPaymentException() {
        VerificationToken vt = buildToken("tok-used", "pay-012", true,
                LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findByToken("tok-used")).thenReturn(Optional.of(vt));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> service.verifyToken("tok-used"));
        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    void verifyToken_idempotent_doesNotDoubleVerify() {
        VerificationToken vt = buildToken("tok-idem", "pay-013", false,
                LocalDateTime.now().plusMinutes(10));

        Payment payment = buildPayment("pay-013", PaymentStatus.COMPLETED);
        payment.setEmailVerified(true); // already verified

        when(tokenRepository.findByToken("tok-idem")).thenReturn(Optional.of(vt));
        when(paymentRepository.findById("pay-013")).thenReturn(Optional.of(payment));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.verifyToken("tok-idem");

        // payment.save should NOT be called because already verified
        verify(paymentRepository, never()).save(any());
    }

    // ─── resendVerification ───────────────────────────────────────────────────

    @Test
    void resendVerification_invalidatesOldTokenAndSendsNew() {
        Payment payment = buildPayment("pay-020", PaymentStatus.COMPLETED);

        VerificationToken existing = buildToken("tok-old", "pay-020", false,
                LocalDateTime.now().plusMinutes(10));

        when(paymentRepository.findById("pay-020")).thenReturn(Optional.of(payment));
        when(tokenRepository.findByPaymentIdAndUsedFalse("pay-020"))
                .thenReturn(Optional.of(existing));
        when(tokenRepository.findAll()).thenReturn(Collections.singletonList(existing));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resendVerification("pay-020", "user@example.com");

        // Old token should be marked used
        assertTrue(existing.isUsed());
        // A new token should be saved (two saves: invalidate old, save new)
        verify(tokenRepository, times(2)).save(any());
        verify(emailService).sendEmail(eq("user@example.com"), contains("Resend"), anyString());
    }

    @Test
    void resendVerification_alreadyVerified_isNoOp() {
        Payment payment = buildPayment("pay-021", PaymentStatus.COMPLETED);
        payment.setEmailVerified(true);

        when(paymentRepository.findById("pay-021")).thenReturn(Optional.of(payment));

        service.resendVerification("pay-021", "user@example.com");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void resendVerification_paymentNotFound_throwsPaymentException() {
        when(paymentRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class,
                () -> service.resendVerification("not-found", "user@example.com"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Payment buildPayment(String id, PaymentStatus status) {
        Payment p = new Payment("rental-1", "user-1", 2500.0, "CARD");
        p.setId(id);
        p.setStatus(status);
        p.setTransactionId("TXN_TEST");
        return p;
    }

    private VerificationToken buildToken(String token, String paymentId,
                                         boolean used, LocalDateTime expiresAt) {
        VerificationToken vt = new VerificationToken(token, paymentId,
                "user-1", "user@example.com", expiresAt);
        vt.setUsed(used);
        return vt;
    }
}
