package com.example.logistics.application.command;

import com.example.logistics.domain.model.Address;
import com.example.logistics.domain.model.DeliveryType;
import com.example.logistics.domain.model.Dimensions;
import com.example.logistics.domain.model.Party;

import java.time.LocalDate;

/**
 * Command carrying the data needed to create a shipment. Encapsulates the validated
 * value objects so the service layer does not have to assemble them from REST types.
 *
 * @param sender              sender party
 * @param recipient           recipient party
 * @param pickupAddress       origin address
 * @param deliveryAddress     destination address
 * @param dimensions          physical dimensions and weight
 * @param deliveryType        selected service level
 * @param estimatedDeliveryDate requested delivery date
 * @param requestedBy         subject (username) issuing the command
 */
public record CreateShipmentCommand(
        Party sender,
        Party recipient,
        Address pickupAddress,
        Address deliveryAddress,
        Dimensions dimensions,
        DeliveryType deliveryType,
        LocalDate estimatedDeliveryDate,
        String requestedBy) {
}
