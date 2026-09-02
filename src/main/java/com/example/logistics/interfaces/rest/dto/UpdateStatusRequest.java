package com.example.logistics.interfaces.rest.dto;

import com.example.logistics.domain.model.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for {@code PATCH /api/v1/shipments/{id}/status}.
 */
public record UpdateStatusRequest(@NotNull ShipmentStatus status, String note) {
}
