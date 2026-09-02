package com.example.logistics.application;

/**
 * Central registry of problem-detail type URIs used by the API error handling layer.
 */
public final class ApiErrors {

    public static final String PROBLEM_BASE = "https://logistics.example.com/problems/";

    public static final String BAD_REQUEST = PROBLEM_BASE + "bad-request";
    public static final String VALIDATION_ERROR = PROBLEM_BASE + "validation-error";
    public static final String UNAUTHORIZED = PROBLEM_BASE + "unauthorized";
    public static final String FORBIDDEN = PROBLEM_BASE + "forbidden";
    public static final String NOT_FOUND = PROBLEM_BASE + "not-found";
    public static final String CONFLICT = PROBLEM_BASE + "conflict";
    public static final String UNPROCESSABLE = PROBLEM_BASE + "unprocessable-entity";
    public static final String TOO_MANY_REQUESTS = PROBLEM_BASE + "too-many-requests";
    public static final String INTERNAL = PROBLEM_BASE + "internal-error";
    public static final String IDEMPOTENCY_REPLAY = PROBLEM_BASE + "idempotency-replay";

    private ApiErrors() {
    }
}
