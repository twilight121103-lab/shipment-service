package com.example.logistics.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Routing/exchange metadata for the shipment events published to RabbitMQ.
 *
 * <p>This maps the logical event types to exchange binding keys, keeping the bindings
 * centralised and overridable via configuration rather than hard-coded in code.
 */
@ConfigurationProperties(prefix = "app.rabbitmq.event")
public record RabbitMqEventProperties(
        String exchange,
        String routingPrefix,
        String queue) {

    /**
     * Returns the routing key used for publishing the given event type,
     * e.g. {@code shipment.shipment.event.ShipmentCreated}.
     */
    public String routingKey(String eventType) {
        return routingPrefix + "." + eventType;
    }
}
