package com.example.logistics.infrastructure.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Declares the RabbitMQ topology: a direct exchange for shipment events, a dead-letter
 * exchange + queue chain for unprocessable messages, and the container configuration.
 *
 * <p>Delivery model: at-least-once. Consumers rely on idempotency to gracefully handle
 * redeliveries. Messages that cannot be processed after N attempts are routed to the
 * DLQ; poison messages never loop forever.
 */
@Configuration
public class RabbitMqTopology {

    public static final String EVENTS_EXCHANGE = "logistics.events.direct";
    public static final String DLX = "logistics.events.dlx";
    public static final String SHIPMENT_QUEUE = "q.shipment.events";
    public static final String SHIPMENT_DLQ = "q.shipment.events.dlq";
    public static final String ROUTING_PREFIX = "shipment.event";

    // Event-type routing keys
    public static final String ROUTING_CREATED = ROUTING_PREFIX + ".ShipmentCreated";
    public static final String ROUTING_STATUS = ROUTING_PREFIX + ".ShipmentStatusChanged";
    public static final String ROUTING_CANCELLED = ROUTING_PREFIX + ".ShipmentCancelled";
    public static final String ROUTING_DELIVERED = ROUTING_PREFIX + ".ShipmentDelivered";

    @Bean
    public Declarable eventsExchange() {
        return new DirectExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Declarable deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Declarable shipmentQueue() {
        return QueueBuilder.durable(SHIPMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", "dead.shipment")
                .build();
    }

    @Bean
    public Declarable shipmentDlq() {
        return QueueBuilder.durable(SHIPMENT_DLQ).build();
    }

    // Bindings: main events
    @Bean
    public Declarable bindingShipmentCreated(DirectExchange eventsExchange, Queue shipmentQueue) {
        return BindingBuilder.bind(shipmentQueue).to(eventsExchange).with(ROUTING_CREATED);
    }

    @Bean
    public Declarable bindingShipmentStatus(DirectExchange eventsExchange, Queue shipmentQueue) {
        return BindingBuilder.bind(shipmentQueue).to(eventsExchange).with(ROUTING_STATUS);
    }

    @Bean
    public Declarable bindingShipmentCancelled(DirectExchange eventsExchange, Queue shipmentQueue) {
        return BindingBuilder.bind(shipmentQueue).to(eventsExchange).with(ROUTING_CANCELLED);
    }

    @Bean
    public Declarable bindingShipmentDelivered(DirectExchange eventsExchange, Queue shipmentQueue) {
        return BindingBuilder.bind(shipmentQueue).to(eventsExchange).with(ROUTING_DELIVERED);
    }

    // DLX -> DLQ
    @Bean
    public Declarable bindingDlxToDlq(DirectExchange deadLetterExchange, Queue shipmentDlq) {
        return BindingBuilder.bind(shipmentDlq).to(deadLetterExchange).with("dead.shipment");
    }

    /**
     * JSON message conversion so payloads carry an explicit consumer-friendly content
     * type (application/json) and type headers.
     */
    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Configures the listener container: JSON conversion, manual ack, a finite prefetch
     * to bound per-consumer load, and a bounded retry that after {@code maxAttempts}
     * rejects the message so it is routed to the DLX/DLQ (no infinite re-processing of
     * poison messages). Ack happens after successful processing, so a crash
     * mid-processing redelivers the message (at-least-once).
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter,
            @Value("${app.rabbitmq.prefetch:10}") int prefetch,
            @Value("${app.rabbitmq.max-attempts:5}") int maxAttempts) {
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(prefetch);
        factory.setDefaultRequeueRejected(false); // reject propagates to DLX/DLQ after retries

        // Bounded, exponential backoff retry. After maxAttempts the message is rejected
        // and routed to the DLX/DLQ (never loops forever).
        factory.setAdviceChain(
                org.springframework.amqp.rabbit.config.RetryInterceptorBuilder.stateless()
                        .maxAttempts(maxAttempts)
                        .backOffOptions(100, 2.0, 5_000)
                        .build());
        return factory;
    }

    /**
     * RabbitTemplate configured to use JSON so the publisher and listeners share a
     * consistent content type. Publish confirmations are enabled in application config.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
