package com.driveaway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

/**
 * PaymentRequest DTO - Data Transfer Object for payment API requests
 * GRASP: DTO Pattern - Separates API layer from internal model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotBlank(message = "Rental ID cannot be blank")
    private String rentalId;
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private Double amount;
    
    @NotBlank(message = "Payment method cannot be blank")
    private String paymentMethod; // CARD, UPI, NETBANKING
    
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
    private String upiId;
    private String netBankingBank;
}