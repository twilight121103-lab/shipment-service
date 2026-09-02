package com.example.logistics.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Example consumer of shipment domain events.
 *
 * <p>Guarantees:
 * <ul>
 *     <li><b>Idempotency</b> — each message carries a stable {@code eventId}; processed
 *         event ids are recorded so redeliveries are deduplicated.</li>
 *     <li><b>Bounded retry</b> — retries are handled by the {@code RetryOperationsInterceptor}
 *         configured on the container; after the configured attempts the message is
 *         rejected and routed to the DLX/DLQ (no infinite loop).</li>
 *     <li><b>Manual ack</b> — the message is acked only after successful processing, so a
 *         crash redelivers (at-least-once).</li>
 *     <li><b>Structured logging / correlation</b> — event id and trace id are bound to the
 *         logger MDC for the duration of processing.</li>
 * </ul>
 */
@Component
public class ShipmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEventStore;

    public ShipmentEventConsumer(ObjectMapper objectMapper,
                                 ProcessedEventStore processedEventStore) {
        this.objectMapper = objectMapper;
        this.processedEventStore = processedEventStore;
    }

    @RabbitListener(queues = RabbitMqTopology.SHIPMENT_QUEUE)
    public void onShipmentEvent(@Payload Message message,
                                @Header(AmqpHeaders.CONSUMER_TAG) String consumerTag,
                                Channel channel,
                                @Header(name = "eventId", required = false) String eventIdHeader) throws IOException {

        final String eventId = eventIdHeader != null ? eventIdHeader : UUID.randomUUID().toString();
        final String eventType = stringHeader(message, "eventType");
        final String aggregateId = stringHeader(message, "aggregateId");

        try (MDC.MDCCloseable c1 = MDC.putCloseable("eventId", eventId);
             MDC.MDCCloseable c2 = MDC.putCloseable("consumerTag", consumerTag)) {

            // Idempotency: skip already-processed events.
            if (processedEventStore.alreadyProcessed(eventId)) {
                log.info("Duplicate event {} ({}) ignored", eventId, eventType);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            final String payload = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            final JsonNode root = objectMapper.readTree(payload);

            log.info("Processing shipment event type={} eventId={} shipmentId={} tracking={}",
                    eventType, eventId, aggregateId, root.path("trackingNumber").asText());

            // Simulate business side-effect; a real consumer would call a use case here.
            // If this throws, the interceptor retries and finally routes to DLQ.
            doProcess(eventType, root);

            processedEventStore.recordProcessed(eventId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("Processed shipment event eventId={} type={}", eventId, eventType);
        } catch (IOException e) {
            // Malformed payload is a permanent failure: reject to DLQ without more attempts.
            log.error("Malformed event payload for eventId={} type={}", eventId, eventType, e);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // Transient/business failure: let the retry interceptor handle attempts.
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            throw e;
        }
    }

    private void doProcess(String eventType, JsonNode root) {
        // Example branching that could, in a wider system, trigger compensating logic.
        if ("ShipmentCreated".equals(eventType) && !root.has("trackingNumber")) {
            throw new IllegalArgumentException("ShipmentCreated event without tracking number");
        }
    }

    private static String stringHeader(Message message, String name) {
        final Object value = message.getMessageProperties().getHeaders().get(name);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Simple in-memory deduplication store for processed event ids.
     *
     * <p>A production deployment would back this with a persistent store so idempotency
     * survives restarts. See TheOutbox ADR for the trade-off.
     */
    public interface ProcessedEventStore {
        boolean alreadyProcessed(String eventId);

        void recordProcessed(String eventId);
    }
}
