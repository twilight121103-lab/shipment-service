package com.example.logistics.infrastructure.messaging;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency store for consumed event ids.
 *
 * <p>Chosen deliberately: the outbox is the source of truth for our own events, and the
 * consumer's real job is to avoid double-*effects*. For this example service an
 * in-memory map is sufficient and keeps the dependency surface small. Swap this bean
 * for a persistent (e.g. Redis/JDBC) implementation when multiple instances must share
 * the deduplication state.
 */
@Component
public class InMemoryProcessedEventStore implements ShipmentEventConsumer.ProcessedEventStore {

    private final Set<String> processed = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public boolean alreadyProcessed(String eventId) {
        return processed.contains(eventId);
    }

    @Override
    public void recordProcessed(String eventId) {
        processed.add(eventId);
    }
}
