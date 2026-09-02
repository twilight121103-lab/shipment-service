package com.example.logistics.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A snapshot of a domain event to be reliably published over messaging.
 *
 * <p>Part of the transactional outbox pattern: an event is persisted in the same
 * database transaction as the corresponding aggregate change, and only afterwards a
 * separate publisher forwards it to the message broker. This eliminates the
 * dual-write problem (DB committed but broker publish failed).
 */
public record OutboxEvent(
        Long id,
        UUID eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        int retryCount,
        Instant nextAttemptAt,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }

    public static OutboxEvent of(String aggregateType, String aggregateId, String eventType, String payload) {
        return new OutboxEvent(
                null,
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                payload,
                OutboxStatus.PENDING,
                0,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now());
    }
}
