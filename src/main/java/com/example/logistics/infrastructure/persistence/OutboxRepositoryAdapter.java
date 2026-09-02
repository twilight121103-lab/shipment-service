package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.model.OutboxEvent;
import com.example.logistics.domain.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA-backed implementation of {@link OutboxRepository} with concurrent-safe claiming.
 */
@Repository
public class OutboxRepositoryAdapter implements OutboxRepository {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryAdapter.class);

    private final OutboxJpaRepository jpaRepository;

    public OutboxRepositoryAdapter(OutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(OutboxEvent event) {
        jpaRepository.save(OutboxJpaEntity.from(event));
    }

    @Override
    @Transactional
    public List<OutboxEvent> claimDue(Instant now, int limit, int retryBackoffSeconds) {
        return jpaRepository.claimDue(now, limit).stream().map(OutboxJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void markPublished(Long id, Instant publishedAt) {
        jpaRepository.findById(id).ifPresent(e -> {
            e.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            e.setPublishedAt(publishedAt);
            e.setUpdatedAt(Instant.now());
            jpaRepository.save(e);
        });
    }

    @Override
    @Transactional
    public void markFailed(Long id) {
        jpaRepository.findById(id).ifPresent(e -> {
            e.setStatus(OutboxEvent.OutboxStatus.FAILED);
            e.setUpdatedAt(Instant.now());
            jpaRepository.save(e);
        });
    }

    @Override
    @Transactional
    public void markFailedRetryable(Long id, Instant nextAttemptAt) {
        jpaRepository.findById(id).ifPresent(e -> {
            e.setStatus(OutboxEvent.OutboxStatus.FAILED);
            e.setRetryCount(e.getRetryCount() + 1);
            e.setNextAttemptAt(nextAttemptAt);
            e.setUpdatedAt(Instant.now());
            jpaRepository.save(e);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public int countPending() {
        return (int) jpaRepository.countUnpublished();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(Long id) {
        return jpaRepository.findById(id).map(OutboxJpaEntity::toDomain);
    }
}
