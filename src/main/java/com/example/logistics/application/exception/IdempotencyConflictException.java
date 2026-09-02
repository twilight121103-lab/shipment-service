package com.example.logistics.application.exception;

import com.example.logistics.application.ApiErrors;

/**
 * Raised when an idempotency key is supplied more than once and the cached response
 * can be replayed, or when the key is already in-flight by another request.
 */
public class IdempotencyConflictException extends DomainServiceException {

    public IdempotencyConflictException(String message) {
        super(message, ApiErrors.IDEMPOTENCY_REPLAY, HttpStatusCode.UNPROCESSABLE_ENTITY);
    }
}
