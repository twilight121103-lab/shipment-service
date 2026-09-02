package com.example.logistics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data repository for outbox events.
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, Long> {

    /**
     * Clams due events using {@code FOR UPDATE SKIP LOCKED} so multiple publisher
     * instances never observe the same row. Only PENDING rows whose retry time has
     * passed (or is null) are eligible.
     */
    @Query(nativeQuery = true, value = """
            SELECT * FROM outbox_events
            WHERE status IN ('PENDING', 'FAILED')
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """)
    List<OutboxJpaEntity> claimDue(@Param("now") java.time.Instant now, @Param("limit") int limit);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM outbox_events WHERE status IN ('PENDING', 'FAILED')")
    long countUnpublished();
}
