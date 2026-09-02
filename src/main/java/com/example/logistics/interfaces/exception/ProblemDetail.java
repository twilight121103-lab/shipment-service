package com.example.logistics.interfaces.exception;

import java.util.List;

/**
 * RFC 7807 Problem Details representation.
 */
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String correlationId,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }
}
