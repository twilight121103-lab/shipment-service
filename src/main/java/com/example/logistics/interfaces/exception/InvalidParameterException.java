package com.example.logistics.interfaces.exception;

import com.example.logistics.application.exception.DomainServiceException;

/**
 * Raised from the REST layer for invalid query/path parameters (400).
 */
public class InvalidParameterException extends DomainServiceException {

    public InvalidParameterException(String message) {
        super(message, com.example.logistics.application.ApiErrors.BAD_REQUEST, HttpStatusCode.BAD_REQUEST);
    }
}
