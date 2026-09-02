package com.example.logistics.application.exception;

import com.example.logistics.application.ApiErrors;

/**
 * Raised when a requested shipment (or other resource) does not exist.
 */
public class ResourceNotFoundException extends DomainServiceException {

    public ResourceNotFoundException(String message) {
        super(message, ApiErrors.NOT_FOUND, HttpStatusCode.NOT_FOUND);
    }
}
