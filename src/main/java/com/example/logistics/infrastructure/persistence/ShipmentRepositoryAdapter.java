package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.model.Shipment;
import com.example.logistics.domain.model.ShipmentStatus;
import com.example.logistics.domain.repository.PageResult;
import com.example.logistics.domain.repository.ShipmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the {@link ShipmentRepository} out-port.
 */
@Repository
public class ShipmentRepositoryAdapter implements ShipmentRepository {

    /** Whitelist of allowed sort properties to avoid injection of arbitrary columns. */
    private static final java.util.Set<String> ALLOWED_SORTS =
            java.util.Set.of("createdAt", "updatedAt", "trackingNumber", "status", "estimatedDeliveryDate");

    private final ShipmentJpaRepository jpaRepository;

    public ShipmentRepositoryAdapter(ShipmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Shipment save(Shipment shipment) {
        final ShipmentJpaEntity saved = jpaRepository.save(ShipmentMapper.toEntity(shipment));
        return ShipmentMapper.toDomain(saved);
    }

    @Override
    public Optional<Shipment> findById(UUID id) {
        return jpaRepository.findById(id).map(ShipmentMapper::toDomain);
    }

    @Override
    public Optional<Shipment> findByTrackingNumber(String trackingNumber) {
        return jpaRepository.findByTrackingNumber(trackingNumber).map(ShipmentMapper::toDomain);
    }

    @Override
    public PageResult<Shipment> findAll(ShipmentStatus status, String trackParam, int page, int size,
                                        String sortBy, String sortDir) {
        final String sortProperty = ALLOWED_SORTS.contains(sortBy) ? sortBy : "createdAt";
        final Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        final Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        final org.springframework.data.domain.Page<ShipmentJpaEntity> result =
                jpaRepository.search(status, trackParam, pageable);

        final var content = result.getContent().stream().map(ShipmentMapper::toDomain).toList();
        return PageResult.of(content, page, size, result.getTotalElements());
    }

    @Override
    public boolean existsByTrackingNumber(String trackingNumber) {
        return jpaRepository.existsByTrackingNumber(trackingNumber);
    }
}
