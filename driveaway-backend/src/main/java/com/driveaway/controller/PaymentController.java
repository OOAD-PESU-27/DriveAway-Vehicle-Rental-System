package com.driveaway.controller;

import com.driveaway.entity.Payment;
import com.driveaway.PaymentStatus;
import com.driveaway.dto.PaymentRequest;
import com.driveaway.dto.PaymentResponse;
import com.driveaway.service.PaymentService;
import com.driveaway.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * PaymentController - Handles HTTP requests related to payments
 * GRASP: Controller Pattern - Handles system events and user requests
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * Process payment endpoint
     * POST /api/v1/payments/process
     */
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            @RequestHeader(value = "X-User-ID", required = true) String userId) {
        try {
            PaymentResponse response = paymentService.processPayment(paymentRequest, userId);
            return response.isSuccess() 
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(
                new PaymentResponse(e.getMessage(), false)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new PaymentResponse("An error occurred while processing payment", false)
            );
        }
    }
    
    /**
     * Get payment by ID endpoint
     * GET /api/v1/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(@PathVariable String paymentId) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            return ResponseEntity.ok(payment);
        } catch (PaymentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get user payments endpoint
     * GET /api/v1/payments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPayments(@PathVariable String userId) {
        try {
            List<Payment> payments = paymentService.getPaymentsByUserId(userId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get payments by status endpoint
     * GET /api/v1/payments/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getPaymentsByStatus(@PathVariable String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            List<Payment> payments = paymentService.getPaymentsByStatus(paymentStatus);
            return ResponseEntity.ok(payments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid payment status");
        }
    }
    
    /**
     * Refund payment endpoint
     * POST /api/v1/payments/{paymentId}/refund
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "X-Admin-ID", required = true) String adminId) {
        try {
            PaymentResponse response = paymentService.refundPayment(paymentId, adminId);
            return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(
                new PaymentResponse(e.getMessage(), false)
            );
        }
    }
}