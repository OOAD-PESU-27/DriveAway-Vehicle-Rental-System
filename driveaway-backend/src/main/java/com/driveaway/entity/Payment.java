package com.driveaway.entity;

import com.driveaway.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Payment Entity - Represents a payment transaction in the system
 * GRASP: Information Expert - Handles payment-related data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {
    
    @Id
    private String id;
    
    private String rentalId;
    private String userId;
    private double amount;
    private String paymentMethod; // CARD, UPI, NETBANKING
    private PaymentStatus status; // PENDING, SUCCESS, FAILED, REFUNDED
    private String transactionId;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String failureReason;
    
    public Payment(String rentalId, String userId, double amount, String paymentMethod) {
        this.rentalId = rentalId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}