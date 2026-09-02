package com.example.logistics.domain.repository;

import com.example.logistics.domain.model.TrackingNumber;

/**
 * Port for generating the next unique tracking number.
 */
public interface TrackingNumberGenerator {

    /**
     * Produces the next tracking number. Implementations must guarantee uniqueness
     * (typically backed by a DB sequence).
     */
    TrackingNumber next();
}
