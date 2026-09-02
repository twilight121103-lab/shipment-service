package com.example.logistics.application.service;

import com.example.logistics.domain.model.OutboxEvent;
import com.example.logistics.domain.repository.MessagePublisher;
import com.example.logistics.domain.repository.OutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Reliable outbox dispatcher.
 *
 * <p>Runs on a schedule, claims due outbox rows, publishes each one to the broker and
 * only then marks it {@code PUBLISHED}. Because claiming is done optimistically with a
 * row-level atomic update, multiple instances can run the publisher safely with no
 * double-publishing of the same event.
 */
@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_BACKOFF_SECONDS = 30;

    private final OutboxRepository outboxRepository;
    private final MessagePublisher messagePublisher;
    private final Counter publishedCounter;
    private final Counter publishFailuresCounter;
    private final Counter permanentFailuresCounter;

    @Autowired
    public OutboxPublisher(OutboxRepository outboxRepository, MessagePublisher messagePublisher,
                          MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.messagePublisher = messagePublisher;
        this.publishedCounter = meterRegistry.counter("outbox.events.published");
        this.publishFailuresCounter = meterRegistry.counter("outbox.events.publish_failures");
        this.permanentFailuresCounter = meterRegistry.counter("outbox.events.permanent_failures");
        Gauge.builder("outbox.events.pending", outboxRepository, OutboxRepository::countPending)
                .register(meterRegistry);
    }

    /**
     * Scheduled tick. The interval is intentionally small for local development; in
     * production the poll scheduling is a small fraction of broker round-trips.
     */
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    public void processPending() {
        final List<OutboxEvent> events = outboxRepository.claimDue(Instant.now(), BATCH_SIZE, RETRY_BACKOFF_SECONDS);
        if (events.isEmpty()) {
            return;
        }
        for (OutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            messagePublisher.publish(event);
            outboxRepository.markPublished(event.id(), Instant.now());
            publishedCounter.increment();
            log.info("Outbox event {} ({}) published", event.eventId(), event.eventType());
        } catch (MessagePublisher.MessagingException e) {
            publishFailuresCounter.increment();
            log.warn("Failed to publish outbox event {} ({}): {}", event.eventId(), event.eventType(),
                    e.getMessage());
            if (event.retryCount() >= MAX_RETRIES) {
                outboxRepository.markFailed(event.id());
                permanentFailuresCounter.increment();
                log.error("Outbox event {} permanently failed after {} attempts", event.eventId(), MAX_RETRIES);
            } else {
                final Instant next = Instant.now().plus(Duration.ofSeconds(RETRY_BACKOFF_SECONDS));
                outboxRepository.markFailedRetryable(event.id(), next);
            }
        }
    }
}
