package com.driveaway.exception;

/**
 * ResourceNotFoundException - Thrown when a requested resource is not found
 * SOLID: SRP - Single responsibility for not-found errors
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}