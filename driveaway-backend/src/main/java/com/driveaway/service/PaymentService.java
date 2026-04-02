package com.driveaway.service;

import com.driveaway.entity.Payment;
import com.driveaway.PaymentStatus;
import com.driveaway.dto.PaymentRequest;
import com.driveaway.dto.PaymentResponse;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PaymentService - Contains business logic for payment processing
 * GRASP: Information Expert - Handles payment-related business logic
 * SOLID: SRP - Only handles payment operations
 */
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ReportService reportService;
    
    /**
     * Process a payment request
     * Implements Observer Pattern - Triggers notification on success
     */
    public PaymentResponse processPayment(PaymentRequest paymentRequest, String userId) {
        try {
            // Validation
            validatePaymentRequest(paymentRequest);
            
            // Create payment entity
            Payment payment = new Payment(
                paymentRequest.getRentalId(),
                userId,
                paymentRequest.getAmount(),
                paymentRequest.getPaymentMethod()
            );
            
            // Generate transaction ID
            String transactionId = generateTransactionId();
            payment.setTransactionId(transactionId);
            
            // Process payment (Simulated)
            boolean isSuccess = processPaymentGateway(paymentRequest, transactionId);
            
            if (isSuccess) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentDate(LocalDateTime.now());
                
                // Save payment
                Payment savedPayment = paymentRepository.save(payment);
                
                // Trigger Observer Pattern - Send notification
                notificationService.sendPaymentSuccessNotification(savedPayment);
                
                // Update reports (Observer Pattern)
                reportService.updateReportOnPaymentSuccess(savedPayment);
                
                // Log audit trail
                auditLogService.logPaymentAction("PAYMENT_SUCCESS", savedPayment.getId(), userId,
                        "Payment processed successfully for amount: " + payment.getAmount());
                
                return new PaymentResponse(
                    savedPayment.getId(),
                    savedPayment.getRentalId(),
                    userId,
                    savedPayment.getAmount(),
                    savedPayment.getPaymentMethod(),
                    PaymentStatus.SUCCESS,
                    transactionId,
                    LocalDateTime.now(),
                    "Payment processed successfully",
                    true
                );
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment gateway declined the transaction");
                
                Payment savedPayment = paymentRepository.save(payment);
                
                // Send failure notification
                notificationService.sendPaymentFailureNotification(savedPayment);
                
                // Log audit trail
                auditLogService.logPaymentAction("PAYMENT_FAILED", savedPayment.getId(), userId,
                        "Payment failed: " + payment.getFailureReason());
                
                return new PaymentResponse(
                    "Payment processing failed. Please try again.",
                    false
                );
            }
        } catch (PaymentException e) {
            auditLogService.logPaymentAction("PAYMENT_ERROR", null, userId,
                    "Payment error: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Validate payment request
     */
    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getAmount() <= 0) {
            throw new PaymentException("Payment amount must be greater than 0");
        }
        
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isEmpty()) {
            throw new PaymentException("Payment method is required");
        }
        
        // Additional validation based on payment method
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
                break;
            default:
                throw new PaymentException("Invalid payment method");
        }
    }
    
    /**
     * Simulate payment gateway processing
     * In real scenario, this would call actual payment gateway (Stripe, PayPal, etc.)
     */
    private boolean processPaymentGateway(PaymentRequest request, String transactionId) {
        // Simulated success rate: 95%
        return Math.random() < 0.95;
    }
    
    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
    
    /**
     * Get payment by ID
     */
    public Payment getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with ID: " + paymentId));
    }
    
    /**
     * Get all payments for a user
     */
    public List<Payment> getPaymentsByUserId(String userId) {
        return paymentRepository.findByUserId(userId);
    }
    
    /**
     * Get payments by status
     */
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }
    
    /**
     * Process refund
     */
    public PaymentResponse refundPayment(String paymentId, String adminId) {
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException("Only successful payments can be refunded");
        }
        
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        Payment refundedPayment = paymentRepository.save(payment);
        
        // Send refund notification
        notificationService.sendRefundNotification(refundedPayment);
        
        // Log audit trail
        auditLogService.logPaymentAction("PAYMENT_REFUNDED", paymentId, adminId,
                "Payment refunded for amount: " + payment.getAmount());
        
        // Update reports
        reportService.updateReportOnPaymentRefund(refundedPayment);
        
        return new PaymentResponse("Payment refunded successfully", true);
    }
}