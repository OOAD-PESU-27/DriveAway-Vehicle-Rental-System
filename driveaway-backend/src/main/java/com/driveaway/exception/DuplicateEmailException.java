package com.driveaway.exception;

/**
 * Thrown when a registration attempt uses an email address that is already registered.
 * Mapped to HTTP 409 Conflict by the global exception handlers.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
