package com.example.logistics.interfaces.rest.dto;

import com.example.logistics.domain.model.Address;
import com.example.logistics.domain.model.DeliveryType;
import com.example.logistics.domain.model.Dimensions;
import com.example.logistics.domain.model.Party;
import com.example.logistics.domain.model.Shipment;
import com.example.logistics.domain.model.ShipmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response representation of a shipment. Encapsulates the domain aggregate and is the
 * only shape returned from the REST API (JPA entities / domain objects are never exposed).
 */
public record ShipmentResponse(
        UUID id,
        String trackingNumber,
        ShipmentStatus status,
        PartyDto sender,
        PartyDto recipient,
        AddressDto pickupAddress,
        AddressDto deliveryAddress,
        DimensionsDto dimensions,
        DeliveryType deliveryType,
        LocalDate estimatedDeliveryDate,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public record PartyDto(String name, String phone, String email) {
        static PartyDto from(Party p) {
            return new PartyDto(p.name(), p.phone(), p.email());
        }
    }

    public record AddressDto(String street, String city, String postalCode, String country) {
        static AddressDto from(Address a) {
            return new AddressDto(a.street(), a.city(), a.postalCode(), a.country());
        }
    }

    public record DimensionsDto(double lengthCm, double widthCm, double heightCm, double weightKg) {
        static DimensionsDto from(Dimensions d) {
            return new DimensionsDto(d.lengthCm(), d.widthCm(), d.heightCm(), d.weightKg());
        }
    }

    public static ShipmentResponse from(Shipment s) {
        final String tracking = s.getTrackingNumber() == null ? null : s.getTrackingNumber().value();
        return new ShipmentResponse(
                s.getId(), tracking, s.getStatus(),
                PartyDto.from(s.getSender()), PartyDto.from(s.getRecipient()),
                AddressDto.from(s.getPickupAddress()), AddressDto.from(s.getDeliveryAddress()),
                DimensionsDto.from(s.getDimensions()),
                s.getDeliveryType(), s.getEstimatedDeliveryDate(),
                s.getCreatedAt(), s.getUpdatedAt(), s.getVersion());
    }

    public static List<ShipmentResponse> list(List<Shipment> shipments) {
        return shipments.stream().map(ShipmentResponse::from).toList();
    }
}
