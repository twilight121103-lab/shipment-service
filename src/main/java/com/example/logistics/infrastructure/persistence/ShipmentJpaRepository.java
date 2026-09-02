package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.model.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository over the JPA entity. Only the persistence adapter talks to it.
 */
public interface ShipmentJpaRepository extends JpaRepository<ShipmentJpaEntity, UUID> {

    Optional<ShipmentJpaEntity> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    /**
     * Filters by optional status and an optional, case-insensitive tracking-number
     * prefix/contains match. Used by the listing endpoint.
     */
    @Query("""
            SELECT s FROM ShipmentJpaEntity s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:track IS NULL OR LOWER(s.trackingNumber) LIKE LOWER(CONCAT('%', :track, '%')))
            """)
    org.springframework.data.domain.Page<ShipmentJpaEntity> search(
            @Param("status") ShipmentStatus status,
            @Param("track") String track,
            org.springframework.data.domain.Pageable pageable);
}
