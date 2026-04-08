package com.driveaway.controller;

import com.driveaway.entity.Payment;
import com.driveaway.PaymentStatus;
import com.driveaway.dto.PaymentRequest;
import com.driveaway.dto.PaymentResponse;
import com.driveaway.service.PaymentService;
import com.driveaway.service.PaymentEmailVerificationService;
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
@CrossOrigin
public class PaymentController {
    
    private final PaymentService paymentService;
    private final PaymentEmailVerificationService emailVerificationService;

    // -------------------------------------------------------------------------
    // Approval-gate workflow endpoints
    // -------------------------------------------------------------------------

    /**
     * Step 1 – Initiate a payment request (creates notification, awaits approval)
     * POST /api/v1/payments/request
     */
    @PostMapping("/request")
    public ResponseEntity<?> initiatePaymentRequest(
            @RequestBody PaymentRequest paymentRequest,
            @RequestHeader(value = "X-User-ID", required = true) String userId) {
        try {
            PaymentResponse response = paymentService.initiatePaymentRequest(paymentRequest, userId);
            return ResponseEntity.ok(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(new PaymentResponse(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new PaymentResponse("An error occurred while initiating payment request", false));
        }
    }

    /**
     * Step 2 – Approve a payment (simulated recipient acceptance)
     * POST /api/v1/payments/{paymentId}/approve
     */
    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<?> approvePayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "X-Approved-By", required = false, defaultValue = "ADMIN") String approvedBy) {
        try {
            PaymentResponse response = paymentService.approvePayment(paymentId, approvedBy);
            return ResponseEntity.ok(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(new PaymentResponse(e.getMessage(), false));
        }
    }

    /**
     * Approve payment via token – POST variant (used by internal tools / API clients)
     * POST /api/v1/payments/approve-by-token?token=APPR_...
     */
    @PostMapping("/approve-by-token")
    public ResponseEntity<?> approveByToken(@RequestParam String token) {
        try {
            PaymentResponse response = paymentService.approvePaymentByToken(token);
            return ResponseEntity.ok(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(new PaymentResponse(e.getMessage(), false));
        }
    }

    /**
     * Approve payment via token – GET variant (used when user clicks the link in the approval email)
     * GET /api/v1/payments/approve-by-token?token=APPR_...
     */
    @GetMapping("/approve-by-token")
    public ResponseEntity<?> approveByTokenGet(@RequestParam String token) {
        try {
            PaymentResponse response = paymentService.approvePaymentByToken(token);
            return ResponseEntity.ok(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(new PaymentResponse(e.getMessage(), false));
        }
    }

    /**
     * Step 3 – Complete payment (only works after approval)
     * POST /api/v1/payments/{paymentId}/complete
     */
    @PostMapping("/{paymentId}/complete")
    public ResponseEntity<?> completePayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "X-User-ID", required = true) String userId) {
        try {
            PaymentResponse response = paymentService.completePayment(paymentId, userId);
            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(new PaymentResponse(e.getMessage(), false));
        }
    }

    // -------------------------------------------------------------------------
    // Legacy / existing endpoints
    // -------------------------------------------------------------------------

    /**
     * Process payment (legacy direct flow)
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
     * Get all payments (admin)
     * GET /api/v1/payments
     */
    @GetMapping
    public ResponseEntity<?> getAllPayments(
            @RequestHeader(value = "X-Admin-ID", required = false) String adminId) {
        try {
            List<Payment> payments = paymentService.getAllPayments();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get payment by ID
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
     * Get user payments
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
     * Get payments by status
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
     * Refund payment
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

    // -------------------------------------------------------------------------
    // Email verification endpoints
    // -------------------------------------------------------------------------

    /**
     * Verify a payment via a one-time email token.
     * GET /api/v1/payments/verify-email?token=...
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            emailVerificationService.verifyToken(token);
            return ResponseEntity.ok("Payment verified successfully.");
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Verification failed: " + e.getMessage());
        }
    }

    /**
     * Resend the payment verification email.
     * POST /api/v1/payments/{paymentId}/resend-verification
     * Body (optional JSON): { "email": "user@example.com" }
     * Header X-User-Email can also be used.
     */
    @PostMapping("/{paymentId}/resend-verification")
    public ResponseEntity<?> resendVerification(
            @PathVariable String paymentId,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        try {
            String userEmail = headerEmail;
            if (userEmail == null && body != null) {
                userEmail = body.get("email");
            }
            if (userEmail == null || userEmail.isBlank()) {
                return ResponseEntity.badRequest().body("User email is required for resend");
            }
            emailVerificationService.resendVerification(paymentId, userEmail);
            return ResponseEntity.ok("Verification email resent successfully.");
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Resend failed: " + e.getMessage());
        }
    }
}
