package com.example.logistics.domain.model;

/**
 * Service level chosen for a shipment, mapping to the expected delivery speed.
 */
public enum DeliveryType {

    /**
     * Flexible window, typically next-business-day.
     */
    STANDARD,

    /**
     * Strengthened window, same or next-day delivery.
     */
    EXPRESS,

    /**
     * Guaranteed arrival within a strict time window on the same day.
     */
    SAME_DAY
}
