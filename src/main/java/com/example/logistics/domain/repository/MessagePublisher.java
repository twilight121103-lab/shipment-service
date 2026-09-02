package com.example.logistics.domain.repository;

import com.example.logistics.domain.model.OutboxEvent;

import java.time.Instant;
import java.util.List;

/**
 * Message publishing port (hexagonal "out" port).
 *
 * <p>Consumers of this interface (the outbox publisher) do not depend on a concrete
 * broker. The messaging adapter behind it is responsible for the actual publish and
 * for surfacing failures as {@code MessagingException} so the caller can react.
 */
public interface MessagePublisher {

    /**
     * Publishes the given outbox event to the broker.
     *
     * @throws MessagingException if the event could not be published.
     */
    void publish(OutboxEvent event);

    /**
     * Raised on broker failures. The caller (outbox dispatcher) uses this to decide
     * whether to retry the event.
     */
    final class MessagingException extends RuntimeException {
        public MessagingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
