package com.example.logistics.domain.exception;

/**
 * Thrown when a shipment status transition violates the state machine business rules.
 */
public class IllegalStateTransitionException extends ShipmentDomainException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
