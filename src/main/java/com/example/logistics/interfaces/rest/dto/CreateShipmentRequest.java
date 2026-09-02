package com.example.logistics.interfaces.rest.dto;

import com.example.logistics.domain.model.DeliveryType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for {@code POST /api/v1/shipments}.
 *
 * <p>Carries Jakarta Bean Validation constraints; the REST layer maps violation errors
 * to a 422 PROBLEM+JSON response. Never bound directly to the domain aggregate
 * (prevents mass-assignment).
 */
public record CreateShipmentRequest(
        @NotNull PartyDto sender,
        @NotNull PartyDto recipient,
        @NotNull AddressDto pickupAddress,
        @NotNull AddressDto deliveryAddress,
        @NotNull DimensionsDto dimensions,
        @NotNull DeliveryType deliveryType,
        @NotNull LocalDate estimatedDeliveryDate) {

    public record PartyDto(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 32) String phone,
            @Email @Size(max = 200) String email) {
    }

    public record AddressDto(
            @NotBlank @Size(max = 200) String street,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 10) String postalCode,
            @NotBlank @Size(max = 3) @Pattern(regexp = "^[A-Za-z]{2,3}$", message = "country must be a 2-3 letter code") String country) {
    }

    public record DimensionsDto(
            @NotNull @Positive @Max(10000) Double lengthCm,
            @NotNull @Positive @Max(10000) Double widthCm,
            @NotNull @Positive @Max(10000) Double heightCm,
            @NotNull @Positive @Max(100000) Double weightKg) {
    }
}
