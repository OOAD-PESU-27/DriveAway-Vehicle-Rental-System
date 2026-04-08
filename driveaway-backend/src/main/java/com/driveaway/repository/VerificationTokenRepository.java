package com.driveaway.repository;

import com.driveaway.entity.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for VerificationToken entities.
 */
@Repository
public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByPaymentIdAndUsedFalse(String paymentId);
}
