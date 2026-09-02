package com.example.logistics.domain.model;

import com.example.logistics.domain.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ShipmentTest {

    private static final Party SENDER = new Party("Alice", "+79000000000", "alice@example.com");
    private static final Party RECIPIENT = new Party("Bob", "+79000000001", "bob@example.com");
    private static final Address PICKUP = new Address("1 Main St", "London", "SW1A 1AA", "GB");
    private static final Address DELIVERY = new Address("2 High St", "Manchester", "M1 1AE", "GB");
    private static final Dimensions DIM = new Dimensions(30, 20, 10, 2.5);
    private static final LocalDate EDD = LocalDate.now().plusDays(3);

    private Shipment newShipment() {
        return Shipment.create(SENDER, RECIPIENT, PICKUP, DELIVERY, DIM,
                DeliveryType.EXPRESS, EDD);
    }

    @Test
    void create_setsCREATEDandUniqueId() {
        Shipment s = newShipment();
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(s.getId()).isNotNull();
        assertThat(s.getCreatedAt()).isNotNull();
        assertThat(s.getUpdatedAt()).isEqualTo(s.getCreatedAt());
    }

    @Test
    void fullHappyPathIsAllowed() {
        Shipment s = newShipment();
        s.assignTrackingNumber(TrackingNumber.of("SLV-2026-000001"));
        assertThatCode(() -> s.changeStatus(ShipmentStatus.CONFIRMED)).doesNotThrowAnyException();
        assertThatCode(() -> s.changeStatus(ShipmentStatus.PICKUP_ASSIGNED)).doesNotThrowAnyException();
        assertThatCode(() -> s.changeStatus(ShipmentStatus.PICKED_UP)).doesNotThrowAnyException();
        assertThatCode(() -> s.changeStatus(ShipmentStatus.IN_TRANSIT)).doesNotThrowAnyException();
        assertThatCode(() -> s.changeStatus(ShipmentStatus.OUT_FOR_DELIVERY)).doesNotThrowAnyException();
        assertThatCode(() -> s.changeStatus(ShipmentStatus.DELIVERED)).doesNotThrowAnyException();
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void deliveredCannotRevertToInTransit() {
        Shipment s = deliver();
        assertThatThrownBy(() -> s.changeStatus(ShipmentStatus.IN_TRANSIT))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void deliveredCannotBeCancelled() {
        Shipment s = deliver();
        assertThatThrownBy(() -> s.cancel("wrong"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void cancelledCannotGetPickupAssigned() {
        Shipment s = newShipment();
        s.cancel("client request");
        assertThatThrownBy(() -> s.changeStatus(ShipmentStatus.PICKUP_ASSIGNED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void repeatingSameTransitionIsRejected() {
        Shipment s = newShipment();
        s.changeStatus(ShipmentStatus.CONFIRMED);
        assertThatThrownBy(() -> s.changeStatus(ShipmentStatus.CONFIRMED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void skippingStateInForwardFlowIsRejected() {
        Shipment s = newShipment();
        // CREATED -> IN_TRANSIT is not a legal single transition.
        assertThatThrownBy(() -> s.changeStatus(ShipmentStatus.IN_TRANSIT))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void cancelIsAllowedBeforeTerminalOnly() {
        Shipment s = newShipment();
        s.changeStatus(ShipmentStatus.CONFIRMED);
        assertThatCode(() -> s.cancel("no longer needed")).doesNotThrowAnyException();
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void deliveryFailureThenReturnOrRedelivery() {
        Shipment s = newShipment();
        s.changeStatus(ShipmentStatus.CONFIRMED);
        s.changeStatus(ShipmentStatus.PICKUP_ASSIGNED);
        s.changeStatus(ShipmentStatus.PICKED_UP);
        s.changeStatus(ShipmentStatus.IN_TRANSIT);
        s.changeStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        s.changeStatus(ShipmentStatus.DELIVERY_FAILED);
        // Redelivery attempt
        assertThatCode(() -> s.changeStatus(ShipmentStatus.OUT_FOR_DELIVERY)).doesNotThrowAnyException();
        s.changeStatus(ShipmentStatus.DELIVERY_FAILED);
        // Or return to sender
        assertThatCode(() -> s.changeStatus(ShipmentStatus.RETURNED)).doesNotThrowAnyException();
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.RETURNED);
    }

    @Test
    void assignTrackingNumberTwiceIsRejected() {
        Shipment s = newShipment();
        s.assignTrackingNumber(TrackingNumber.of("SLV-2026-000001"));
        assertThatThrownBy(() -> s.assignTrackingNumber(TrackingNumber.of("SLV-2026-000002")))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    private Shipment deliver() {
        Shipment s = newShipment();
        s.changeStatus(ShipmentStatus.CONFIRMED);
        s.changeStatus(ShipmentStatus.PICKUP_ASSIGNED);
        s.changeStatus(ShipmentStatus.PICKED_UP);
        s.changeStatus(ShipmentStatus.IN_TRANSIT);
        s.changeStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        s.changeStatus(ShipmentStatus.DELIVERED);
        return s;
    }
}
