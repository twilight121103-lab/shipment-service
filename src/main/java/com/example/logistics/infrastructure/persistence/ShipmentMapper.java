package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.model.Address;
import com.example.logistics.domain.model.Dimensions;
import com.example.logistics.domain.model.Party;
import com.example.logistics.domain.model.Shipment;
import com.example.logistics.domain.model.TrackingNumber;

/**
 * Converts between the domain aggregate and the JPA entity. Keeping the mapping here
 * avoids leaking persistence concerns into the domain or application layers.
 */
final class ShipmentMapper {

    private ShipmentMapper() {
    }

    static Shipment toDomain(ShipmentJpaEntity e) {
        return Shipment.reconstitute(
                e.getId(),
                e.getTrackingNumber() == null ? null : TrackingNumber.of(e.getTrackingNumber()),
                e.getStatus(),
                new Party(e.getSenderName(), e.getSenderPhone(), e.getSenderEmail()),
                new Party(e.getRecipientName(), e.getRecipientPhone(), e.getRecipientEmail()),
                new Address(e.getPickupStreet(), e.getPickupCity(), e.getPickupPostalCode(), e.getPickupCountry()),
                new Address(e.getDeliveryStreet(), e.getDeliveryCity(), e.getDeliveryPostalCode(), e.getDeliveryCountry()),
                new Dimensions(e.getLengthCm(), e.getWidthCm(), e.getHeightCm(), e.getWeightKg()),
                e.getDeliveryType(),
                e.getEstimatedDeliveryDate(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion());
    }

    static ShipmentJpaEntity toEntity(Shipment s) {
        final ShipmentJpaEntity e = new ShipmentJpaEntity();
        e.setId(s.getId());
        e.setTrackingNumber(s.getTrackingNumber() == null ? null : s.getTrackingNumber().value());
        e.setStatus(s.getStatus());
        e.setSenderName(s.getSender().name());
        e.setSenderPhone(s.getSender().phone());
        e.setSenderEmail(s.getSender().email());
        e.setRecipientName(s.getRecipient().name());
        e.setRecipientPhone(s.getRecipient().phone());
        e.setRecipientEmail(s.getRecipient().email());
        e.setPickupStreet(s.getPickupAddress().street());
        e.setPickupCity(s.getPickupAddress().city());
        e.setPickupPostalCode(s.getPickupAddress().postalCode());
        e.setPickupCountry(s.getPickupAddress().country());
        e.setDeliveryStreet(s.getDeliveryAddress().street());
        e.setDeliveryCity(s.getDeliveryAddress().city());
        e.setDeliveryPostalCode(s.getDeliveryAddress().postalCode());
        e.setDeliveryCountry(s.getDeliveryAddress().country());
        e.setLengthCm(s.getDimensions().lengthCm());
        e.setWidthCm(s.getDimensions().widthCm());
        e.setHeightCm(s.getDimensions().heightCm());
        e.setWeightKg(s.getDimensions().weightKg());
        e.setDeliveryType(s.getDeliveryType());
        e.setEstimatedDeliveryDate(s.getEstimatedDeliveryDate());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        e.setVersion(s.getVersion());
        return e;
    }
}
