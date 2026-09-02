package com.example.logistics.application.command;

import com.example.logistics.domain.model.ShipmentStatus;

import java.util.UUID;

/**
 * Command to change a shipment's status.
 */
public record ChangeStatusCommand(
        UUID shipmentId,
        ShipmentStatus newStatus,
        String initiatedBy) {
}
