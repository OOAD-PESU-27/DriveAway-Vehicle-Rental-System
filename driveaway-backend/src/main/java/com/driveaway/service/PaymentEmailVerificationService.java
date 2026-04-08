package com.driveaway.service;

import com.driveaway.NotificationType;
import com.driveaway.entity.Booking;
import com.driveaway.entity.Notification;
import com.driveaway.entity.Payment;
import com.driveaway.entity.Vehicle;
import com.driveaway.entity.VerificationToken;
import com.driveaway.exception.PaymentException;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.NotificationRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final VerificationTokenRepository tokenRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // -------------------------------------------------------------------------
    // 1. Send verification email after successful payment
    // -------------------------------------------------------------------------

    /**
     * Generates a verification token, persists it, and sends a confirmation+verification email.
     * Delegates to {@link #sendVerificationEmail(Payment, String, String)} with no user name.
     */
    public void sendVerificationEmail(Payment payment, String userEmail) {
        sendVerificationEmail(payment, userEmail, null);
    }

    /**
     * Generates a verification token, persists it, and sends a confirmation+verification email.
     * Safe to call multiple times – if the payment is already verified, this is a no-op.
     *
     * @param payment   the completed/successful payment
     * @param userEmail recipient email address
     * @param userName  registered user's display name (may be null)
     */
    public void sendVerificationEmail(Payment payment, String userEmail, String userName) {
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
        String subject = "DriveAway – Payment Confirmation & Verification (Booking #" + payment.getRentalId() + ")";
        String body = buildVerificationEmailBody(payment, verifyLink, userName);

        try {
            emailService.sendEmail(userEmail, subject, body);
            persistVerificationNotification(payment, userEmail, verifyLink);
            markEmailSent(payment);
            log.info("[VERIFICATION] Confirmation+verification email sent for payment={} to={}", payment.getId(), userEmail);
        } catch (Exception e) {
            log.error("[VERIFICATION] Failed to send confirmation+verification email for payment={}: {}",
                    payment.getId(), e.getMessage(), e);
            auditLogService.logPaymentAction("VERIFICATION_EMAIL_ERROR", payment.getId(),
                    payment.getUserId(), "Failed to send confirmation+verification email: " + e.getMessage());
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
        String body = buildVerificationEmailBody(payment, verifyLink, null);

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

    /**
     * Builds the payment confirmation + verification email body.
     * Includes user name (if available), booking/vehicle details (if available),
     * amount, payment date/time, and a verification link.
     */
    private String buildVerificationEmailBody(Payment payment, String verifyLink, String userName) {
        String greeting = (userName != null && !userName.isBlank())
                ? "Hello " + userName + ","
                : "Hello,";

        String bookingId = payment.getRentalId() != null ? payment.getRentalId() : "N/A";
        String txnId = payment.getTransactionId() != null ? payment.getTransactionId() : "N/A";
        String paymentDate = payment.getPaymentDate() != null
                ? payment.getPaymentDate().format(DATE_FORMATTER)
                : LocalDateTime.now().format(DATE_FORMATTER);

        // Look up vehicle info via the booking (best-effort; gracefully skipped on any error)
        String vehicleInfo = resolveVehicleInfo(bookingId);

        StringBuilder body = new StringBuilder();
        body.append(greeting).append("%n%n");
        body.append("Your payment has been processed successfully. Here is your booking summary:%n%n");
        body.append("  Booking ID   : ").append(bookingId).append("%n");
        if (!vehicleInfo.isEmpty()) {
            body.append("  Vehicle      : ").append(vehicleInfo).append("%n");
        }
        body.append("  Amount Paid  : ₹").append(String.format("%.2f", payment.getAmount())).append("%n");
        body.append("  Payment Date : ").append(paymentDate).append("%n");
        body.append("  Transaction  : ").append(txnId).append("%n%n");
        body.append("To verify your payment, please click the link below:%n%n");
        body.append("  ").append(verifyLink).append("%n%n");
        body.append("This link will expire in ").append(TOKEN_VALIDITY_MINUTES).append(" minutes.%n%n");
        body.append("If you did not make this payment, please contact us immediately.%n%n");
        body.append("Thank you,%n");
        body.append("DriveAway Team");

        return String.format(body.toString());
    }

    /**
     * Resolves a human-readable vehicle description from a booking/rental ID.
     * Returns an empty string if the booking or vehicle cannot be found.
     */
    private String resolveVehicleInfo(String rentalId) {
        if (rentalId == null || rentalId.isBlank()) {
            return "";
        }
        try {
            return bookingRepository.findById(rentalId)
                    .flatMap(booking -> vehicleRepository.findById(booking.getVehicleId()))
                    .map(vehicle -> vehicle.getBrand() + " " + vehicle.getModel()
                            + " (" + vehicle.getVehicleType() + ")")
                    .orElse("");
        } catch (Exception e) {
            log.warn("[VERIFICATION] Could not resolve vehicle info for rentalId={}: {}", rentalId, e.getMessage());
            return "";
        }
    }
}
