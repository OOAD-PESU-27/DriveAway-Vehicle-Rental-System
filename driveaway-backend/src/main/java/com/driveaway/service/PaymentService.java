package com.driveaway.service;

import com.driveaway.entity.Payment;
import com.driveaway.entity.User;
import com.driveaway.PaymentStatus;
import com.driveaway.dto.PaymentRequest;
import com.driveaway.dto.PaymentResponse;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.UserRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentService - Contains business logic for payment processing
 * GRASP: Information Expert - Handles payment-related business logic
 * SOLID: SRP - Only handles payment operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final PaymentEmailVerificationService emailVerificationService;
    private final BookingRepository bookingRepository;

    // -------------------------------------------------------------------------
    // Approval-gate workflow (Option B - simulated, no real SMTP required)
    // Flow: initiatePaymentRequest -> approvePayment -> completePayment
    // -------------------------------------------------------------------------

    /**
     * Step 1: Initiate a payment request.
     * Creates a payment in REQUESTED state and sends a simulated "request" notification.
     */
    public PaymentResponse initiatePaymentRequest(PaymentRequest paymentRequest, String userId) {
        if (paymentRequest.getAmount() <= 0) {
            throw new PaymentException("Payment amount must be greater than 0");
        }

        Payment payment = new Payment(
                paymentRequest.getRentalId(),
                userId,
                paymentRequest.getAmount(),
                paymentRequest.getPaymentMethod()
        );
        payment.setStatus(PaymentStatus.REQUESTED);
        payment.setRequestedAt(LocalDateTime.now());

        // Handle optional security deposit
        if (paymentRequest.getSecurityDeposit() != null && paymentRequest.getSecurityDeposit() > 0) {
            payment.setSecurityDeposit(paymentRequest.getSecurityDeposit());
            payment.setSecurityDepositStatus("HELD");
        }

        // Generate a simulated approval token
        String approvalToken = "APPR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        payment.setApprovalToken(approvalToken);

        Payment savedPayment = paymentRepository.save(payment);

        // Send simulated "payment request sent" notification
        notificationService.sendPaymentRequestNotification(savedPayment, approvalToken);

        auditLogService.logPaymentAction("PAYMENT_REQUESTED", savedPayment.getId(), userId,
                "Payment request initiated for amount: " + payment.getAmount());

        return buildResponse(savedPayment, "Payment request submitted. Awaiting approval.", true);
    }

    /**
     * Step 2: Approve the payment (simulated recipient acceptance).
     * Can be triggered by paymentId + approvedBy.
     * Idempotent: if already APPROVED, returns a success response without re-applying the transition.
     */
    public PaymentResponse approvePayment(String paymentId, String approvedBy) {
        Payment payment = getPaymentById(paymentId);

        // Idempotent: already approved – return success without re-running the transition
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            log.info("[PAYMENT] approvePayment called on already-APPROVED payment={} – returning idempotent success", paymentId);
            return buildResponse(payment, "Payment already approved. You can now complete the payment.", true);
        }

        if (payment.getStatus() != PaymentStatus.REQUESTED &&
                payment.getStatus() != PaymentStatus.PENDING_APPROVAL) {
            throw new PaymentException("Payment is not in a state that can be approved. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.APPROVED);
        payment.setApprovedAt(LocalDateTime.now());
        payment.setApprovedBy(approvedBy);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        notificationService.sendPaymentApprovalNotification(savedPayment);

        auditLogService.logPaymentAction("PAYMENT_APPROVED", paymentId, approvedBy,
                "Payment approved for amount: " + payment.getAmount());

        return buildResponse(savedPayment, "Payment approved successfully. You can now complete the payment.", true);
    }

    /**
     * Approve payment via token (simulated email link acceptance).
     */
    public PaymentResponse approvePaymentByToken(String approvalToken) {
        Payment payment = paymentRepository.findByApprovalToken(approvalToken)
                .orElseThrow(() -> new PaymentException("Invalid or expired approval token"));
        return approvePayment(payment.getId(), "SYSTEM_TOKEN");
    }

    /**
     * Step 3: Complete/process payment - only allowed after APPROVED status.
     * Idempotent: if already COMPLETED or SUCCESS, returns a success response without re-processing.
     */
    public PaymentResponse completePayment(String paymentId, String userId) {
        Payment payment = getPaymentById(paymentId);

        // Idempotent: already completed – return success without re-processing.
        // SUCCESS is treated equivalently to COMPLETED: it is a backward-compatibility alias
        // for the same terminal state (see PaymentStatus enum).
        if (payment.getStatus() == PaymentStatus.COMPLETED ||
                payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[PAYMENT] completePayment called on already-COMPLETED payment={} – returning idempotent success", paymentId);
            return buildResponse(payment, "Payment already completed.", true);
        }

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new PaymentException("Payment must be approved before it can be completed. Current status: " + payment.getStatus());
        }

        // Simulate payment gateway
        String transactionId = generateTransactionId();
        payment.setTransactionId(transactionId);
        boolean isSuccess = processPaymentGateway();

        if (isSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            if ("HELD".equals(payment.getSecurityDepositStatus())) {
                payment.setSecurityDepositStatus("RELEASED");
            }
            payment.setUpdatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            notificationService.sendPaymentSuccessNotification(savedPayment);
            reportService.updateReportOnPaymentSuccess(savedPayment);

            auditLogService.logPaymentAction("PAYMENT_COMPLETED", savedPayment.getId(), userId,
                    "Payment completed successfully for amount: " + payment.getAmount());

            // Persist paid amount back to booking so the bookings grid shows a non-zero amount
            updateBookingPaidAmount(savedPayment);

            // Trigger email verification
            triggerEmailVerification(savedPayment);

            return buildResponse(savedPayment, "Payment completed successfully.", true);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment gateway declined the transaction");
            payment.setUpdatedAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            notificationService.sendPaymentFailureNotification(savedPayment);

            auditLogService.logPaymentAction("PAYMENT_FAILED", savedPayment.getId(), userId,
                    "Payment failed: " + payment.getFailureReason());

            return buildResponse(savedPayment, "Payment processing failed. Please try again.", false);
        }
    }

    // -------------------------------------------------------------------------
    // Legacy direct-process flow (kept for backward compatibility)
    // -------------------------------------------------------------------------

    /**
     * Process a payment request (legacy direct flow)
     */
    public PaymentResponse processPayment(PaymentRequest paymentRequest, String userId) {
        try {
            validatePaymentRequest(paymentRequest);
            
            Payment payment = new Payment(
                paymentRequest.getRentalId(),
                userId,
                paymentRequest.getAmount(),
                paymentRequest.getPaymentMethod()
            );
            
            if (paymentRequest.getSecurityDeposit() != null && paymentRequest.getSecurityDeposit() > 0) {
                payment.setSecurityDeposit(paymentRequest.getSecurityDeposit());
                payment.setSecurityDepositStatus("HELD");
            }

            String transactionId = generateTransactionId();
            payment.setTransactionId(transactionId);
            
            boolean isSuccess = processPaymentGateway();
            
            if (isSuccess) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentDate(LocalDateTime.now());
                if ("HELD".equals(payment.getSecurityDepositStatus())) {
                    payment.setSecurityDepositStatus("RELEASED");
                }
                
                Payment savedPayment = paymentRepository.save(payment);
                notificationService.sendPaymentSuccessNotification(savedPayment);
                reportService.updateReportOnPaymentSuccess(savedPayment);
                auditLogService.logPaymentAction("PAYMENT_SUCCESS", savedPayment.getId(), userId,
                        "Payment processed successfully for amount: " + payment.getAmount());

                // Persist paid amount back to booking so the bookings grid shows a non-zero amount
                updateBookingPaidAmount(savedPayment);

                // Trigger email verification
                triggerEmailVerification(savedPayment);

                return buildResponse(savedPayment, "Payment processed successfully", true);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment gateway declined the transaction");
                
                Payment savedPayment = paymentRepository.save(payment);
                notificationService.sendPaymentFailureNotification(savedPayment);
                auditLogService.logPaymentAction("PAYMENT_FAILED", savedPayment.getId(), userId,
                        "Payment failed: " + payment.getFailureReason());
                
                return new PaymentResponse("Payment processing failed. Please try again.", false);
            }
        } catch (PaymentException e) {
            auditLogService.logPaymentAction("PAYMENT_ERROR", null, userId,
                    "Payment error: " + e.getMessage());
            throw e;
        }
    }
    
    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getAmount() <= 0) {
            throw new PaymentException("Payment amount must be greater than 0");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isEmpty()) {
            throw new PaymentException("Payment method is required");
        }
        switch (request.getPaymentMethod().toUpperCase()) {
            case "CARD":
                if (request.getCardNumber() == null || request.getCardNumber().isEmpty()) {
                    throw new PaymentException("Card number is required for card payment");
                }
                break;
            case "UPI":
                if (request.getUpiId() == null || request.getUpiId().isEmpty()) {
                    throw new PaymentException("UPI ID is required for UPI payment");
                }
                break;
            case "NETBANKING":
                if (request.getNetBankingBank() == null || request.getNetBankingBank().isEmpty()) {
                    throw new PaymentException("Bank selection is required for NetBanking");
                }
                if (request.getNetBankingAccountHolder() == null || request.getNetBankingAccountHolder().isBlank()) {
                    throw new PaymentException("Account holder name is required for NetBanking");
                }
                if (request.getNetBankingAccountNumber() == null || request.getNetBankingAccountNumber().isBlank()) {
                    throw new PaymentException("Account number is required for NetBanking");
                }
                if (request.getNetBankingIfscCode() == null || request.getNetBankingIfscCode().isBlank()) {
                    throw new PaymentException("IFSC code is required for NetBanking");
                }
                break;
            default:
                throw new PaymentException("Invalid payment method");
        }
    }
    
    private boolean processPaymentGateway() {
        // Simulated success rate: 95%
        return Math.random() < 0.95;
    }
    
    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
    
    public Payment getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with ID: " + paymentId));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    public List<Payment> getPaymentsByUserId(String userId) {
        return paymentRepository.findByUserId(userId);
    }
    
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }
    
    public PaymentResponse refundPayment(String paymentId, String adminId) {
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != PaymentStatus.SUCCESS &&
                payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("Only successful/completed payments can be refunded");
        }
        
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        Payment refundedPayment = paymentRepository.save(payment);
        
        notificationService.sendRefundNotification(refundedPayment);
        auditLogService.logPaymentAction("PAYMENT_REFUNDED", paymentId, adminId,
                "Payment refunded for amount: " + payment.getAmount());
        reportService.updateReportOnPaymentRefund(refundedPayment);
        
        return new PaymentResponse("Payment refunded successfully", true);
    }

    /**
     * Refund (or forfeit) the security deposit after a vehicle return and damage check.
     * @param bookingId    the booking/rental ID that was paid
     * @param damageCharge the charge assessed for damage (0 = no damage)
     * @param adminId      staff/admin performing the action
     * @return response describing the deposit outcome
     */
    public PaymentResponse refundSecurityDeposit(String bookingId, double damageCharge, String adminId) {
        List<Payment> payments = paymentRepository.findByRentalId(bookingId);
        Payment payment = payments.stream()
                .filter(p -> p.getSecurityDeposit() > 0 && "HELD".equals(p.getSecurityDepositStatus()))
                .findFirst()
                .orElseThrow(() -> new PaymentException(
                        "No held security deposit found for booking: " + bookingId));

        double deposit = payment.getSecurityDeposit();
        double refundAmount = Math.max(0, deposit - damageCharge);

        if (refundAmount <= 0) {
            payment.setSecurityDepositStatus("FORFEITED");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            auditLogService.logPaymentAction("DEPOSIT_FORFEITED", payment.getId(), adminId,
                    "Security deposit forfeited due to damage charge: " + damageCharge);
            return new PaymentResponse("Security deposit forfeited due to damage charges", true);
        } else {
            payment.setSecurityDepositStatus("REFUNDED");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            auditLogService.logPaymentAction("DEPOSIT_REFUNDED", payment.getId(), adminId,
                    "Security deposit refunded: " + refundAmount + " (damage: " + damageCharge + ")");
            notificationService.sendRefundNotification(payment);
            return new PaymentResponse(
                    "Security deposit refunded: ₹" + String.format("%.2f", refundAmount) +
                    (damageCharge > 0 ? " (₹" + String.format("%.2f", damageCharge) + " deducted for damage)" : ""),
                    true);
        }
    }

    private PaymentResponse buildResponse(Payment payment, String message, boolean success) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRentalId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getPaymentDate(),
                message,
                success
        );
    }

    /**
     * Looks up the user's name and email, then triggers the confirmation+verification email flow.
     * Errors are logged but do not propagate – payment is already saved.
     */
    private void triggerEmailVerification(Payment payment) {
        try {
            Optional<User> userOpt = userRepository.findById(payment.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                emailVerificationService.sendVerificationEmail(
                        payment, user.getEmail(), user.getName());
            } else {
                log.warn("[PAYMENT] User not found for payment={} userId={} – skipping confirmation email",
                        payment.getId(), payment.getUserId());
            }
        } catch (Exception e) {
            log.error("[PAYMENT] Could not trigger confirmation email for payment={}: {}",
                    payment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Best-effort update: sets the paid amount on the associated booking record so the
     * bookings list reflects the actual amount paid rather than ₹0.
     * Errors are logged but never propagate – the payment record is already saved.
     */
    private void updateBookingPaidAmount(Payment payment) {
        String rentalId = payment.getRentalId();
        if (rentalId == null || rentalId.isBlank()) {
            log.warn("[PAYMENT] Payment {} has no rentalId – skipping booking paidAmount update", payment.getId());
            return;
        }
        try {
            bookingRepository.findById(rentalId).ifPresent(booking -> {
                booking.setPaidAmount(payment.getAmount());
                // Advance booking to ACTIVE to reflect that payment has been made;
                // CONFIRMED means booked-but-unpaid, ACTIVE means paid-and-ongoing.
                if ("CONFIRMED".equals(booking.getStatus())) {
                    booking.setStatus("ACTIVE");
                    bookingRepository.save(booking);
                    log.info("[PAYMENT] Updated paidAmount={} and status=ACTIVE on booking={}",
                            payment.getAmount(), rentalId);
                } else {
                    bookingRepository.save(booking);
                    log.info("[PAYMENT] Updated paidAmount={} on booking={} (status unchanged: {})",
                            payment.getAmount(), rentalId, booking.getStatus());
                }
            });
        } catch (Exception e) {
            log.error("[PAYMENT] Could not update paidAmount on booking={}: {}", rentalId, e.getMessage(), e);
        }
    }
}
