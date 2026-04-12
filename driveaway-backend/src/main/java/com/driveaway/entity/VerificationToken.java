package com.driveaway.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * VerificationToken - Stores one-time email verification tokens for payments.
 * Each token is linked to a payment, has an expiry, and a used flag to
 * ensure idempotency and prevent replay attacks.
 */
@Data
@NoArgsConstructor
@Document(collection = "verification_tokens")
public class VerificationToken {

    @Id
    private String id;

    /** The secure random token sent in the verification link. */
    private String token;

    /** Payment this token belongs to. */
    private String paymentId;

    /** User who made the payment. */
    private String userId;

    /** Email address the verification was sent to. */
    private String userEmail;

    /** Token becomes invalid after this time. */
    private LocalDateTime expiresAt;

    /** True once the token has been consumed (prevents reuse). */
    private boolean used;

    private LocalDateTime createdAt;

    /** Track how many times a resend has been requested. */
    private int resendCount;

    public VerificationToken(String token, String paymentId, String userId,
                             String userEmail, LocalDateTime expiresAt) {
        this.token = token;
        this.paymentId = paymentId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = LocalDateTime.now();
        this.resendCount = 0;
    }
}
