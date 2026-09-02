package com.example.logistics.application.command;

import java.util.UUID;

/**
 * Command to cancel an existing shipment.
 */
public record CancelShipmentCommand(UUID shipmentId, String reason, String initiatedBy) {
}
