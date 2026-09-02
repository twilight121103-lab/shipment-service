package com.example.logistics.domain.repository;

import com.example.logistics.domain.model.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbox port used by the publisher to claim and mark domain events.
 */
public interface OutboxRepository {

    void save(OutboxEvent event);

    /**
     * Atomically claims up to {@code limit} PENDING or retry-eligible events whose
     * {@code nextAttemptAt} has passed, marking them with a processing owner so
     * concurrent publishers do not double-publish.
     */
    List<OutboxEvent> claimDue(Instant now, int limit, int retryBackoffSeconds);

    /**
     * Marks an event as published.
     */
    void markPublished(Long id, Instant publishedAt);

    /**
     * Marks an event as failed (permanent) after exhausting retries.
     */
    void markFailed(Long id);

    void markFailedRetryable(Long id, Instant nextAttemptAt);

    int countPending();

    Optional<OutboxEvent> findById(Long id);
}
