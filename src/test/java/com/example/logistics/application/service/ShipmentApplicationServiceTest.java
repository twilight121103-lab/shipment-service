package com.example.logistics.application.service;

import com.example.logistics.application.command.CreateShipmentCommand;
import com.example.logistics.application.exception.IdempotencyConflictException;
import com.example.logistics.domain.model.Address;
import com.example.logistics.domain.model.DeliveryType;
import com.example.logistics.domain.model.Dimensions;
import com.example.logistics.domain.model.Party;
import com.example.logistics.domain.model.TrackingNumber;
import com.example.logistics.domain.repository.IdempotencyStore;
import com.example.logistics.domain.repository.OutboxRepository;
import com.example.logistics.domain.repository.ShipmentRepository;
import com.example.logistics.domain.repository.TrackingNumberGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentApplicationServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @Mock TrackingNumberGenerator trackingNumberGenerator;
    @Mock OutboxRepository outboxRepository;
    @Mock IdempotencyStore idempotencyStore;

    ShipmentApplicationService service;

    @BeforeEach
    void setUp() {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ShipmentApplicationService(shipmentRepository, trackingNumberGenerator,
                outboxRepository, idempotencyStore, mapper);
    }

    private CreateShipmentCommand command() {
        return new CreateShipmentCommand(
                new Party("A", "1", "a@b.com"),
                new Party("B", "2", "b@b.com"),
                new Address("1 St", "London", "SW1A 1AA", "GB"),
                new Address("2 St", "Manchester", "M1 1AE", "GB"),
                new Dimensions(10, 10, 10, 1),
                DeliveryType.STANDARD,
                LocalDate.now().plusDays(2),
                "user");
    }

    @Test
    void createStoresShipmentAndOutboxEvent() {
        when(trackingNumberGenerator.next()).thenReturn(TrackingNumber.of("SLV-2026-000001"));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createShipment(command(), null);

        assertThat(result).isInstanceOf(ShipmentApplicationService.ShipmentAction.Created.class);
        var created = ((ShipmentApplicationService.ShipmentAction.Created) result).shipment();
        assertThat(created.getTrackingNumber().value()).isEqualTo("SLV-2026-000001");
        assertThat(created.getStatus()).isEqualTo(com.example.logistics.domain.model.ShipmentStatus.CREATED);
        verify(shipmentRepository).save(any());
        verify(outboxRepository).save(any());
    }

    @Test
    void createWithoutIdempotencyNeverTalksToStore() {
        when(trackingNumberGenerator.next()).thenReturn(TrackingNumber.of("SLV-2026-000042"));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createShipment(command(), null);

        verifyNoInteractions(idempotencyStore);
    }

    @Test
    void completedIdempotencyKeyReturnsReplayWithoutCreatingSecondShipment() {
        when(idempotencyStore.find("key-1"))
                .thenReturn(Optional.of(new IdempotencyStore.Completed("key-1", UUID.randomUUID().toString(), null)));

        var result = service.createShipment(command(), "key-1");

        assertThat(result).isInstanceOf(ShipmentApplicationService.ShipmentAction.Replay.class);
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(outboxRepository);
    }

    @Test
    void inFlightIdempotencyKeyThrowsConflict() {
        when(idempotencyStore.find("key-2"))
                .thenReturn(Optional.of(new IdempotencyStore.InFlight("key-2")));

        assertThatThrownBy(() -> service.createShipment(command(), "key-2"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void createCompletesIdempotencyRecord() {
        when(trackingNumberGenerator.next()).thenReturn(TrackingNumber.of("SLV-2026-000099"));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencyStore.putIfAbsent(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(true);

        service.createShipment(command(), "key-3");

        verify(idempotencyStore).complete(anyString(), anyString(), any());
    }
}
