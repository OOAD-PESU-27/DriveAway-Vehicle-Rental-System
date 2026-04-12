package com.driveaway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.driveaway.PaymentStatus;
import java.time.LocalDateTime;

/**
 * PaymentResponse DTO - Data Transfer Object for payment API responses
 * GRASP: DTO Pattern - Separates API layer from internal model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private String paymentId;
    private String rentalId;
    private String userId;
    private Double amount;
    private String paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime paymentDate;
    private String message;
    private boolean success;
    
    public PaymentResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}