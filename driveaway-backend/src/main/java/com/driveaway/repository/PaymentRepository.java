package com.driveaway.repository;

import com.driveaway.entity.Payment;
import com.driveaway.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PaymentRepository - Handles database operations for payments
 * GRASP: Repository Pattern - Abstracts database operations
 */
@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    Optional<Payment> findByApprovalToken(String approvalToken);
    
    List<Payment> findByUserId(String userId);
    
    List<Payment> findByRentalId(String rentalId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    long countByStatus(PaymentStatus status);
}