package com.example.logistics.interfaces.exception;

import com.example.logistics.application.exception.DomainServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Global exception handler producing consistent {@code application/problem+json}
 * responses. Internal details (stack traces, sql messages) are never leaked to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_BASE = "https://logistics.example.com/problems/";

    /** Handles all exceptions from the application/domain layers, incl. StateTransition (409) and Idempotency (422). */
    @ExceptionHandler(DomainServiceException.class)
    ResponseEntity<ProblemDetail> handleDomainService(DomainServiceException ex, WebRequest request) {
        final HttpStatus status = HttpStatus.resolve(ex.getHttpStatus()) != null
                ? HttpStatus.resolve(ex.getHttpStatus()) : HttpStatus.INTERNAL_SERVER_ERROR;
        return build(ex.getMessage(), status, ex.getProblemType(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, WebRequest request) {
        return build("Malformed request: " + ex.getMessage(), HttpStatus.BAD_REQUEST,
                PROBLEM_BASE + "bad-request", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        final List<ProblemDetail.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        return build("Request validation failed", HttpStatus.UNPROCESSABLE_ENTITY,
                PROBLEM_BASE + "validation-error", request, errors);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return build("The shipment was modified concurrently. Re-fetch and retry.",
                HttpStatus.CONFLICT, PROBLEM_BASE + "conflict", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return build("You do not have permission to perform this action.", HttpStatus.FORBIDDEN,
                PROBLEM_BASE + "forbidden", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return build("Authentication required or token invalid.", HttpStatus.UNAUTHORIZED,
                PROBLEM_BASE + "unauthorized", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, WebRequest request) {
        return build("Resource not found.", HttpStatus.NOT_FOUND, PROBLEM_BASE + "not-found", request);
    }

    // Catch-all: log the full error server-side but never expose details to the client.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unhandled exception while processing {}", request.getDescription(false), ex);
        return build("An internal error occurred.", HttpStatus.INTERNAL_SERVER_ERROR,
                PROBLEM_BASE + "internal-error", request);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ResponseEntity<ProblemDetail> build(String detail, HttpStatus status, String type,
                                                WebRequest request) {
        return build(detail, status, type, request, List.of());
    }

    private ResponseEntity<ProblemDetail> build(String detail, HttpStatus status, String type,
                                                WebRequest request, List<ProblemDetail.FieldError> errors) {
        final ProblemDetail problem = new ProblemDetail(
                type,
                status.getReasonPhrase(),
                status.value(),
                detail,
                request.getDescription(false),
                MDC.get("correlationId"),
                errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private ProblemDetail.FieldError toFieldError(FieldError fe) {
        return new ProblemDetail.FieldError(fe.getField(), Optional.ofNullable(fe.getDefaultMessage())
                .orElse("invalid value"));
    }
}
