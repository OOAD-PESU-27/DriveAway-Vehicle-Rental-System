package com.driveaway.service;

import com.driveaway.NotificationType;
import com.driveaway.entity.Notification;
import com.driveaway.entity.Payment;
import com.driveaway.entity.VerificationToken;
import com.driveaway.exception.PaymentException;
import com.driveaway.repository.NotificationRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentEmailVerificationService - End-to-end email verification flow.
 *
 * Flow:
 *   1. sendVerificationEmail(payment, userEmail) – called on payment success:
 *      generates token, persists VerificationToken, sends email, marks Payment.emailVerificationSent.
 *   2. verifyToken(token) – user clicks link:
 *      validates token (expiry + used), marks Payment.emailVerified, invalidates token.
 *   3. resendVerification(paymentId, userEmail) – user requests resend:
 *      invalidates old active token, generates new one, re-sends email.
 *
 * Idempotency:
 *   - If Payment.emailVerified is already true, verifyToken returns immediately.
 *   - If a valid unused token already exists on resend, it is invalidated first.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEmailVerificationService {

    private static final int TOKEN_VALIDITY_MINUTES = 30;
    private static final int MAX_RESENDS = 5;

    private final VerificationTokenRepository tokenRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // -------------------------------------------------------------------------
    // 1. Send verification email after successful payment
    // -------------------------------------------------------------------------

    /**
     * Generates a verification token, persists it, and sends a verification email.
     * Safe to call multiple times – if the payment is already verified, this is a no-op.
     *
     * @param payment   the completed/successful payment
     * @param userEmail recipient email address
     */
    public void sendVerificationEmail(Payment payment, String userEmail) {
        if (payment.isEmailVerified()) {
            log.info("[VERIFICATION] Payment {} already verified – skipping email send", payment.getId());
            return;
        }

        if (userEmail == null || userEmail.isBlank()) {
            log.warn("[VERIFICATION] No email address for payment {} – skipping verification email", payment.getId());
            return;
        }

        String token = generateToken();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES);
        VerificationToken vt = new VerificationToken(token, payment.getId(),
                payment.getUserId(), userEmail, expiry);
        tokenRepository.save(vt);

        String verifyLink = baseUrl + "/api/v1/payments/verify-email?token=" + token;
        String subject = "DriveAway – Verify your payment (Booking #" + payment.getRentalId() + ")";
        String body = buildVerificationEmailBody(payment, verifyLink);

        try {
            emailService.sendEmail(userEmail, subject, body);
            persistVerificationNotification(payment, userEmail, verifyLink);
            markEmailSent(payment);
            log.info("[VERIFICATION] Verification email sent for payment={} to={}", payment.getId(), userEmail);
        } catch (Exception e) {
            log.error("[VERIFICATION] Failed to send verification email for payment={}: {}",
                    payment.getId(), e.getMessage(), e);
            auditLogService.logPaymentAction("VERIFICATION_EMAIL_ERROR", payment.getId(),
                    payment.getUserId(), "Failed to send verification email: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 2. Verify a token
    // -------------------------------------------------------------------------

    /**
     * Validates a verification token:
     * <ul>
     *   <li>Token must exist</li>
     *   <li>Token must not be expired</li>
     *   <li>Token must not have been used before</li>
     * </ul>
     * On success the token is marked used and the payment is marked emailVerified.
     *
     * @param token the raw token string from the verification link
     * @throws PaymentException on invalid/expired/used token
     */
    public void verifyToken(String token) {
        VerificationToken vt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new PaymentException("Invalid verification token"));

        if (vt.isUsed()) {
            throw new PaymentException("Verification token has already been used");
        }

        if (LocalDateTime.now().isAfter(vt.getExpiresAt())) {
            throw new PaymentException("Verification token has expired. Please request a new one.");
        }

        // Mark token as consumed
        vt.setUsed(true);
        tokenRepository.save(vt);

        // Mark payment as verified
        Payment payment = paymentRepository.findById(vt.getPaymentId())
                .orElseThrow(() -> new PaymentException("Payment not found for token"));

        if (!payment.isEmailVerified()) {
            payment.setEmailVerified(true);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        persistVerifiedNotification(payment, vt.getUserEmail());
        auditLogService.logPaymentAction("PAYMENT_EMAIL_VERIFIED", payment.getId(),
                payment.getUserId(), "Email verified for payment by " + vt.getUserEmail());

        log.info("[VERIFICATION] Payment {} verified via token by email={}", payment.getId(), vt.getUserEmail());
    }

    // -------------------------------------------------------------------------
    // 3. Resend verification
    // -------------------------------------------------------------------------

    /**
     * Resends the verification email for a payment.
     * <ul>
     *   <li>If already verified – no-op</li>
     *   <li>Invalidates any existing active token first</li>
     *   <li>Enforces a maximum resend count to prevent abuse</li>
     * </ul>
     *
     * @param paymentId the payment to resend verification for
     * @param userEmail recipient email address
     */
    public void resendVerification(String paymentId, String userEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));

        if (payment.isEmailVerified()) {
            log.info("[VERIFICATION] Payment {} already verified – resend skipped", paymentId);
            return;
        }

        // Invalidate any existing unused token and check resend cap
        tokenRepository.findByPaymentIdAndUsedFalse(paymentId).ifPresent(existing -> {
            if (existing.getResendCount() >= MAX_RESENDS) {
                throw new PaymentException("Maximum resend limit reached for payment: " + paymentId);
            }
            existing.setUsed(true);
            tokenRepository.save(existing);
        });

        String token = generateToken();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES);
        VerificationToken vt = new VerificationToken(token, paymentId,
                payment.getUserId(), userEmail, expiry);
        // Track resend count
        long previousResends = countPreviousTokens(paymentId);
        vt.setResendCount((int) previousResends);
        tokenRepository.save(vt);

        String verifyLink = baseUrl + "/api/v1/payments/verify-email?token=" + token;
        String subject = "DriveAway – Resend: Verify your payment (Booking #" + payment.getRentalId() + ")";
        String body = buildVerificationEmailBody(payment, verifyLink);

        try {
            emailService.sendEmail(userEmail, subject, body);
            auditLogService.logPaymentAction("VERIFICATION_EMAIL_RESENT", paymentId,
                    payment.getUserId(), "Verification email resent to " + userEmail);
            log.info("[VERIFICATION] Verification email resent for payment={} to={}", paymentId, userEmail);
        } catch (Exception e) {
            log.error("[VERIFICATION] Failed to resend verification email for payment={}: {}",
                    paymentId, e.getMessage(), e);
            throw new PaymentException("Failed to resend verification email: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void markEmailSent(Payment payment) {
        payment.setEmailVerificationSent(true);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private long countPreviousTokens(String paymentId) {
        // approximation: every token for this payment counts as a resend
        return tokenRepository.findAll().stream()
                .filter(t -> paymentId.equals(t.getPaymentId()))
                .count();
    }

    private void persistVerificationNotification(Payment payment, String userEmail, String verifyLink) {
        try {
            Notification n = new Notification(
                    payment.getUserId(),
                    payment.getId(),
                    NotificationType.PAYMENT_EMAIL_VERIFICATION,
                    "Please verify your payment of ₹" + payment.getAmount()
                            + ". Click: " + verifyLink + " (expires in " + TOKEN_VALIDITY_MINUTES + " min)",
                    "DriveAway – Verify your payment"
            );
            n.setNotificationChannel("EMAIL");
            n.setStatus("SENT");
            notificationRepository.save(n);
        } catch (Exception e) {
            log.warn("[VERIFICATION] Could not persist verification notification for payment={}: {}",
                    payment.getId(), e.getMessage());
        }
    }

    private void persistVerifiedNotification(Payment payment, String userEmail) {
        try {
            Notification n = new Notification(
                    payment.getUserId(),
                    payment.getId(),
                    NotificationType.PAYMENT_VERIFIED,
                    "Your payment of ₹" + payment.getAmount() + " has been successfully verified.",
                    "DriveAway – Payment Verified"
            );
            n.setNotificationChannel("EMAIL");
            n.setStatus("SENT");
            notificationRepository.save(n);
        } catch (Exception e) {
            log.warn("[VERIFICATION] Could not persist verified notification for payment={}: {}",
                    payment.getId(), e.getMessage());
        }
    }

    private String buildVerificationEmailBody(Payment payment, String verifyLink) {
        return String.format(
                "Hello,%n%n" +
                "Your payment of ₹%.2f for booking #%s has been processed successfully.%n%n" +
                "Please click the link below to verify your payment:%n%n" +
                "  %s%n%n" +
                "This link will expire in %d minutes.%n%n" +
                "Transaction ID: %s%n%n" +
                "If you did not make this payment, please contact us immediately.%n%n" +
                "Thank you,%n" +
                "DriveAway Team",
                payment.getAmount(),
                payment.getRentalId() != null ? payment.getRentalId() : "N/A",
                verifyLink,
                TOKEN_VALIDITY_MINUTES,
                payment.getTransactionId() != null ? payment.getTransactionId() : "N/A"
        );
    }
}
