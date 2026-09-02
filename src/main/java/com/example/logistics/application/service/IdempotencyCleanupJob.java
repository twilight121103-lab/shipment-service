package com.example.logistics.application.service;

import com.example.logistics.domain.repository.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Periodically removes expired idempotency-key rows, keeping the table bounded.
 */
@Service
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyStore idempotencyStore;

    public IdempotencyCleanupJob(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Scheduled(fixedDelayString = "${app.idempotency.cleanup-interval-ms:3600000}")
    public void cleanup() {
        final int deleted = idempotencyStore.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Removed {} expired idempotency records", deleted);
        }
    }
}
