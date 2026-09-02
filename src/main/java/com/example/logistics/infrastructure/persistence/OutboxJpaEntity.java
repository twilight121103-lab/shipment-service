package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.model.OutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA representation of an outbox event row.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxEvent.OutboxStatus status = OutboxEvent.OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutboxJpaEntity() {
        // for JPA
    }

    public static OutboxJpaEntity from(OutboxEvent e) {
        final OutboxJpaEntity entity = new OutboxJpaEntity();
        entity.eventId = e.eventId();
        entity.aggregateType = e.aggregateType();
        entity.aggregateId = e.aggregateId();
        entity.eventType = e.eventType();
        entity.payload = e.payload();
        entity.status = e.status();
        entity.retryCount = e.retryCount();
        entity.nextAttemptAt = e.nextAttemptAt();
        entity.publishedAt = e.publishedAt();
        entity.createdAt = e.createdAt();
        entity.updatedAt = e.updatedAt();
        return entity;
    }

    public OutboxEvent toDomain() {
        return new OutboxEvent(
                id, eventId, aggregateType, aggregateId, eventType, payload,
                status, retryCount, nextAttemptAt, publishedAt, createdAt, updatedAt);
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEvent.OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxEvent.OutboxStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
