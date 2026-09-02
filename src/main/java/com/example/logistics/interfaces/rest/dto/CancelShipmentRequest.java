package com.example.logistics.interfaces.rest.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for {@code POST /api/v1/shipments/{id}/cancel}.
 */
public record CancelShipmentRequest(
        @Size(max = 500) String reason) {
}
