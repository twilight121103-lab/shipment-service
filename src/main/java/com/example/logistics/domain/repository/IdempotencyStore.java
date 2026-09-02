package com.example.logistics.domain.repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Port for idempotency key storage. Used by the create-shipment flow to guarantee a
 * client-supplied {@code Idempotency-Key} produces exactly one shipment even when a
 * request is retried (e.g. after a network timeout) or sent concurrently.
 */
public interface IdempotencyStore {

    /**
     * Registers a new idempotency key. Returns {@code false} if the key already exists.
     *
     * <p>Implementation must enforce uniqueness atomically so concurrent requests with
     * the same key converge on a single outcome.
     */
    boolean putIfAbsent(String key, String resourceType, String resourceId, String responseBody,
                        Instant expiresAt);

    Optional<IdempotencyRecord> find(String key);

    /** Marks the registered record with the resulting resource id and cached response. */
    void complete(String key, String resourceId, String responseBody);

    /** Removes expired records. Called periodically by a cleanup job. */
    int deleteExpired(Instant now);

    /**
     * Record describing a registered idempotency key.
     */
    sealed interface IdempotencyRecord permits InFlight, Completed {}

    record InFlight(String key) implements IdempotencyRecord {}

    record Completed(String key, String resourceId, String responseBody) implements IdempotencyRecord {}
}
