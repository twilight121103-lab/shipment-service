package com.example.logistics.domain.repository;

import com.example.logistics.domain.model.Shipment;
import com.example.logistics.domain.model.ShipmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port (hexagonal "out" port) for persisting {@link Shipment} aggregates.
 *
 * <p>The domain layer depends only on this interface, never on JPA or Spring Data.
 * The infrastructure layer provides a concrete JPA-backed implementation.
 */
public interface ShipmentRepository {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(UUID id);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /**
     * Finds shipments applying a status filter and ordering. The caller supplies a
     * pageable descriptor to keep persistence details out of the domain.
     */
    PageResult<Shipment> findAll(ShipmentStatus status, String trackParam, int page, int size, String sortBy, String sortDir);

    boolean existsByTrackingNumber(String trackingNumber);
}
