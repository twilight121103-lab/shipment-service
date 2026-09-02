package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.repository.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * PostgreSQL-backed {@link IdempotencyStore} relying on a unique constraint and
 * {@code INSERT ... ON CONFLICT DO NOTHING} for atomic, race-free registration.
 */
@Repository
public class IdempotencyStoreAdapter implements IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyStoreAdapter.class);

    private final IdempotencyJpaRepository jpaRepository;

    public IdempotencyStoreAdapter(IdempotencyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public boolean putIfAbsent(String key, String resourceType, String resourceId, String responseBody,
                               Instant expiresAt) {
        try {
            int inserted = jpaRepository.insertIfAbsent(key, resourceType, resourceId, responseBody, expiresAt);
            return inserted > 0;
        } catch (DataIntegrityViolationException e) {
            // Rare path: inserted by a concurrent transaction just now.
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> find(String key) {
        return jpaRepository.findByIdempotencyKey(key).map(e ->
                e.getState() == IdempotencyJpaEntity.State.COMPLETED
                        ? new Completed(e.getIdempotencyKey(), e.getResourceId(), e.getResponseBody())
                        : new InFlight(e.getIdempotencyKey()));
    }

    @Override
    @Transactional
    public void complete(String key, String resourceId, String responseBody) {
        jpaRepository.findByIdempotencyKey(key).ifPresent(e -> {
            e.setState(IdempotencyJpaEntity.State.COMPLETED);
            e.setResourceId(resourceId);
            e.setResponseBody(responseBody);
            jpaRepository.save(e);
        });
    }

    @Override
    @Transactional
    public int deleteExpired(Instant now) {
        // Returns the number of removed rows; implemented as native delete for efficiency.
        return jpaRepository.deleteExpired(now);
    }
}
