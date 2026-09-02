package com.example.logistics.domain.exception;

/**
 * Base runtime exception for the domain layer.
 */
public class ShipmentDomainException extends RuntimeException {

    public ShipmentDomainException(String message) {
        super(message);
    }

    public ShipmentDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
