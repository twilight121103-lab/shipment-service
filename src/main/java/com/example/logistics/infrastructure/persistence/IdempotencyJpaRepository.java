package com.example.logistics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyJpaEntity, Long> {

    Optional<IdempotencyJpaEntity> findByIdempotencyKey(String key);

    /**
     * Atomically inserts a new key, doing nothing if it already exists. Returns the
     * number of rows actually inserted (0 means the key was already present).
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO idempotency_keys
                (idempotency_key, resource_type, resource_id, response_body, state, created_at, expires_at)
            VALUES
                (:key, :resourceType, :resourceId, :responseBody, 'IN_FLIGHT', now(), :expiresAt)
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    int insertIfAbsent(@Param("key") String key,
                       @Param("resourceType") String resourceType,
                       @Param("resourceId") String resourceId,
                       @Param("responseBody") String responseBody,
                       @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM idempotency_keys WHERE expires_at < :now")
    int deleteExpired(@Param("now") Instant now);
}
