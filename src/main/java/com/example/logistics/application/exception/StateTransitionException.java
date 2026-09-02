package com.example.logistics.application.exception;

import com.example.logistics.domain.exception.IllegalStateTransitionException;

/**
 * Adapts the domain's state-machine violation into a framework-friendly conflict.
 * The {@link com.example.logistics.interfaces.exception.GlobalExceptionHandler} maps
 * this to HTTP 409.
 */
public class StateTransitionException extends DomainServiceException {

    public StateTransitionException(IllegalStateTransitionException cause) {
        super(cause.getMessage(), com.example.logistics.application.ApiErrors.CONFLICT, HttpStatusCode.CONFLICT);
    }
}
