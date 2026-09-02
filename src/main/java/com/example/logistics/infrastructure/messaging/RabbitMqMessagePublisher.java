package com.example.logistics.infrastructure.messaging;

import com.example.logistics.domain.model.OutboxEvent;
import com.example.logistics.domain.repository.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes an outbox event to RabbitMQ using publish-confirmations so a successful
 * return guarantees the broker accepted the message. The outbox publisher treats any
 * failure as retryable.
 */
@Component
public class RabbitMqMessagePublisher implements MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(OutboxEvent event) {
        try {
            final MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            props.setHeader("eventType", event.eventType());
            props.setHeader("aggregateType", event.aggregateType());
            props.setHeader("aggregateId", event.aggregateId());
            props.setHeader("eventId", event.eventId().toString());
            props.setMessageId(event.eventId().toString());

            final Message message = org.springframework.amqp.core.MessageBuilder
                    .withBody(event.payload().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .andProperties(props)
                    .build();

            rabbitTemplate.convertAndSend(RabbitMqTopology.EVENTS_EXCHANGE,
                    RabbitMqTopology.ROUTING_PREFIX + "." + event.eventType(), message);
            // With publish-confirmations enabled, a successful send means the broker acked.
        } catch (AmqpException e) {
            throw new MessagingException(
                    "Failed to publish outbox event " + event.eventId() + " to RabbitMQ", e);
        }
    }
}
