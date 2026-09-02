package com.example.logistics.application.service;

import com.example.logistics.application.command.CancelShipmentCommand;
import com.example.logistics.application.command.ChangeStatusCommand;
import com.example.logistics.application.command.CreateShipmentCommand;
import com.example.logistics.application.events.ShipmentDomainEvent;
import com.example.logistics.application.exception.IdempotencyConflictException;
import com.example.logistics.application.exception.ResourceNotFoundException;
import com.example.logistics.application.exception.StateTransitionException;
import com.example.logistics.application.query.ShipmentQuery;
import com.example.logistics.domain.exception.IllegalStateTransitionException;
import com.example.logistics.domain.model.OutboxEvent;
import com.example.logistics.domain.model.Shipment;
import com.example.logistics.domain.model.ShipmentStatus;
import com.example.logistics.domain.model.TrackingNumber;
import com.example.logistics.domain.repository.IdempotencyStore;
import com.example.logistics.domain.repository.OutboxRepository;
import com.example.logistics.domain.repository.PageResult;
import com.example.logistics.domain.repository.ShipmentRepository;
import com.example.logistics.domain.repository.TrackingNumberGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application-сервис, оркестрирующий use cases для отправлений.
 *
 * <p>Этот класс владеет границами транзакций. Каждый мутирующий use case выполняется в
 * одном методе {@code @Transactional}, где агрегат, событие outbox и запись
 * идемпотентности сохраняются вместе (паттерн транзакционного outbox), что гарантирует
 * надёжную публикацию событий.
 */
@Service
@Transactional
public class ShipmentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentApplicationService.class);

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final long IDEMPOTENCY_TTL_SECONDS = 24 * 60 * 60; // 24ч

    private final ShipmentRepository shipmentRepository;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final OutboxRepository outboxRepository;
    private final IdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    public ShipmentApplicationService(ShipmentRepository shipmentRepository,
                                      TrackingNumberGenerator trackingNumberGenerator,
                                      OutboxRepository outboxRepository,
                                      IdempotencyStore idempotencyStore,
                                      ObjectMapper objectMapper) {
        this.shipmentRepository = shipmentRepository;
        this.trackingNumberGenerator = trackingNumberGenerator;
        this.outboxRepository = outboxRepository;
        this.idempotencyStore = idempotencyStore;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // Создание (с идемпотентностью)
    // ------------------------------------------------------------------

    /**
     * Создаёт новое отправление. Если передан {@code idempotencyKey} и предыдущий вызов
     * с тем же ключом уже создал отправление, этот вызов не повторяется — возвращается
     * исходный {@code resourceId}, чтобы вызывающая сторона могла воспроизвести результат.
     *
     * <p>Параллельные вызовы с одним ключом: {@link IdempotencyStore#putIfAbsent}
     * (реализовано через {@code INSERT ... ON CONFLICT DO NOTHING}) гарантирует, что
     * победит только один; проигравший получает {@link IdempotencyConflictException}.
     */
    public ShipmentAction createShipment(CreateShipmentCommand command, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = idempotencyStore.find(idempotencyKey);
            if (existing.isPresent()) {
                if (existing.get() instanceof IdempotencyStore.Completed completed) {
                    log.info("Replaying idempotent create for key {}", idempotencyKey);
                    return new ShipmentAction.Replay(completed.resourceId());
                }
                if (existing.get() instanceof IdempotencyStore.InFlight inFlight) {
                    throw new IdempotencyConflictException("Idempotency key '%s' is being processed"
                            .formatted(idempotencyKey));
                }
            }
        }

        final Shipment shipment = Shipment.create(
                command.sender(),
                command.recipient(),
                command.pickupAddress(),
                command.deliveryAddress(),
                command.dimensions(),
                command.deliveryType(),
                command.estimatedDeliveryDate());

        if (idempotencyKey != null) {
            boolean acquired = idempotencyStore.putIfAbsent(
                    idempotencyKey, AGGREGATE_TYPE, shipment.getId().toString(), null,
                    Instant.now().plusSeconds(IDEMPOTENCY_TTL_SECONDS));
            if (!acquired) {
                throw new IdempotencyConflictException(
                        "Idempotency key '%s' is being processed concurrently".formatted(idempotencyKey));
            }
        }

        final TrackingNumber trackingNumber = trackingNumberGenerator.next();
        shipment.assignTrackingNumber(trackingNumber);

        shipmentRepository.save(shipment);

        outboxRepository.save(OutboxEvent.of(
                AGGREGATE_TYPE, shipment.getId().toString(), "ShipmentCreated",
                serialize(ShipmentDomainEvent.from(shipment, null, "ShipmentCreated"))));

        if (idempotencyKey != null) {
            idempotencyStore.complete(idempotencyKey, shipment.getId().toString(), null);
        }

        log.info("Shipment created id={} tracking={} by={}",
                shipment.getId(), trackingNumber.value(), command.requestedBy());
        return new ShipmentAction.Created(shipment);
    }

    // ------------------------------------------------------------------
    // Смена статуса
    // ------------------------------------------------------------------

    public Shipment changeStatus(ChangeStatusCommand command) {
        final Shipment shipment = requireShipment(command.shipmentId());
        final ShipmentStatus previous = shipment.getStatus();
        try {
            shipment.changeStatus(command.newStatus());
        } catch (IllegalStateTransitionException e) {
            throw new StateTransitionException(e);
        }
        shipmentRepository.save(shipment);
        publishStatusChange(shipment, previous);
        log.info("Shipment {} status {}->{} by={}", shipment.getId(), previous, shipment.getStatus(),
                command.initiatedBy());
        return shipment;
    }

    // ------------------------------------------------------------------
    // Отмена
    // ------------------------------------------------------------------

    public Shipment cancel(CancelShipmentCommand command) {
        final Shipment shipment = requireShipment(command.shipmentId());
        final ShipmentStatus previous = shipment.getStatus();
        try {
            shipment.cancel(command.reason());
        } catch (IllegalStateTransitionException e) {
            throw new StateTransitionException(e);
        }
        shipmentRepository.save(shipment);
        outboxRepository.save(OutboxEvent.of(
                AGGREGATE_TYPE, shipment.getId().toString(), "ShipmentCancelled",
                serialize(ShipmentDomainEvent.from(shipment, previous, "ShipmentCancelled"))));
        log.info("Shipment {} cancelled by={} reason='{}'", shipment.getId(), command.initiatedBy(),
                command.reason());
        return shipment;
    }

    // ------------------------------------------------------------------
    // Запросы (Queries)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Shipment getById(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment '" + id + "' not found"));
    }

    @Transactional(readOnly = true)
    public Shipment getByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment with tracking number '" + trackingNumber + "' not found"));
    }

    @Transactional(readOnly = true)
    public PageResult<Shipment> search(ShipmentQuery query) {
        return shipmentRepository.findAll(
                query.status(), query.trackingNumberLike(), query.page(), query.size(),
                query.sortBy(), query.sortDirection().name());
    }

    // ------------------------------------------------------------------
    // Вспомогательные методы
    // ------------------------------------------------------------------

    private Shipment requireShipment(UUID id) {
        return getById(id);
    }

    private void publishStatusChange(Shipment shipment, ShipmentStatus previous) {
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            outboxRepository.save(OutboxEvent.of(
                    AGGREGATE_TYPE, shipment.getId().toString(), "ShipmentDelivered",
                    serialize(ShipmentDomainEvent.from(shipment, previous, "ShipmentDelivered"))));
        }
        outboxRepository.save(OutboxEvent.of(
                AGGREGATE_TYPE, shipment.getId().toString(), "ShipmentStatusChanged",
                serialize(ShipmentDomainEvent.from(shipment, previous, "ShipmentStatusChanged"))));
    }

    private String serialize(ShipmentDomainEvent event) {
        var p = switch (event) {
            case ShipmentDomainEvent.ShipmentCreated e -> e.payload();
            case ShipmentDomainEvent.ShipmentCancelled e -> e.payload();
            case ShipmentDomainEvent.ShipmentDelivered e -> e.payload();
            case ShipmentDomainEvent.ShipmentStatusChanged e -> e.payload();
        };
        try {
            return objectMapper.writeValueAsString(p);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event " + event.type(), e);
        }
    }

    /**
     * Result of a create-shipment command.
     */
    public sealed interface ShipmentAction {
        record Created(Shipment shipment) implements ShipmentAction {}

        record Replay(String resourceId) implements ShipmentAction {}
    }
}
