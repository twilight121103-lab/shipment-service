package com.example.logistics.application.events;

import com.example.logistics.domain.model.Shipment;
import com.example.logistics.domain.model.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain events raised by the application service whenever a shipment changes.
 * These are not Spring events — they are pure data snapshots that are persisted to
 * the outbox and forwarded to the message broker.
 */
public sealed interface ShipmentDomainEvent {

    String type();

    UUID shipmentId();

    String trackingNumber();

    Instant occurredAt();

    /**
     * Serializable (JSON-friendly) payload. Keeps the messaging format independent of
     * internal domain types so the contract can evolve.
     */
    record Payload(UUID shipmentId, String trackingNumber, ShipmentStatus status, String previousStatus,
                   Instant occurredAt) {
    }

    record ShipmentCreated(Payload payload) implements ShipmentDomainEvent {
        @Override public String type() { return "ShipmentCreated"; }
        @Override public UUID shipmentId() { return payload.shipmentId(); }
        @Override public String trackingNumber() { return payload.trackingNumber(); }
        @Override public Instant occurredAt() { return payload.occurredAt(); }
    }

    record ShipmentStatusChanged(Payload payload) implements ShipmentDomainEvent {
        @Override public String type() { return "ShipmentStatusChanged"; }
        @Override public UUID shipmentId() { return payload.shipmentId(); }
        @Override public String trackingNumber() { return payload.trackingNumber(); }
        @Override public Instant occurredAt() { return payload.occurredAt(); }
    }

    record ShipmentCancelled(Payload payload) implements ShipmentDomainEvent {
        @Override public String type() { return "ShipmentCancelled"; }
        @Override public UUID shipmentId() { return payload.shipmentId(); }
        @Override public String trackingNumber() { return payload.trackingNumber(); }
        @Override public Instant occurredAt() { return payload.occurredAt(); }
    }

    record ShipmentDelivered(Payload payload) implements ShipmentDomainEvent {
        @Override public String type() { return "ShipmentDelivered"; }
        @Override public UUID shipmentId() { return payload.shipmentId(); }
        @Override public String trackingNumber() { return payload.trackingNumber(); }
        @Override public Instant occurredAt() { return payload.occurredAt(); }
    }

    /** Maps an event to a {@code ShipmentDomainEvent} subtype based on its type name. */
    static ShipmentDomainEvent from(Shipment shipment, ShipmentStatus previousStatus, String eventType) {
        final var payload = new Payload(
                shipment.getId(),
                shipment.getTrackingNumber().value(),
                shipment.getStatus(),
                previousStatus == null ? null : previousStatus.name(),
                Instant.now());
        return switch (eventType) {
            case "ShipmentCreated" -> new ShipmentCreated(payload);
            case "ShipmentCancelled" -> new ShipmentCancelled(payload);
            case "ShipmentDelivered" -> new ShipmentDelivered(payload);
            default -> new ShipmentStatusChanged(payload);
        };
    }
}
