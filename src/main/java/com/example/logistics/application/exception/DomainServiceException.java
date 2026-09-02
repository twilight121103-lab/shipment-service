package com.example.logistics.application.exception;

import com.example.logistics.domain.exception.ShipmentDomainException;

/**
 * Base class for exceptions raised from the application service layer.
 * Carries a problem-detail {@code type} URI and an HTTP status which the interface
 * (REST) layer maps to a PROBLEM+JSON response.
 */
public class DomainServiceException extends ShipmentDomainException {

    private final String problemType;
    private final int httpStatus;

    public DomainServiceException(String message, String problemType, HttpStatusCode httpStatus) {
        super(message);
        this.problemType = problemType;
        this.httpStatus = httpStatus.code();
    }

    public String getProblemType() {
        return problemType;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public enum HttpStatusCode {
        BAD_REQUEST(400),
        UNAUTHORIZED(401),
        FORBIDDEN(403),
        NOT_FOUND(404),
        CONFLICT(409),
        UNPROCESSABLE_ENTITY(422),
        TOO_MANY_REQUESTS(429);

        private final int code;

        HttpStatusCode(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }
}
