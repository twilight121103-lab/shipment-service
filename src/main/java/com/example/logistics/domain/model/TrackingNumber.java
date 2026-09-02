package com.example.logistics.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a tracking number, e.g. {@code SLV-2024-000001}.
 *
 * <p>The format is centralised here so it is produced and validated in exactly one
 * place. It is immutable and comparable by value.
 */
public record TrackingNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2,4}-\\d{4}-\\d{6,}$");

    public TrackingNumber {
        Objects.requireNonNull(value, "trackingNumber must not be null");
        final String trimmed = value.trim().toUpperCase();
        if (!FORMAT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Tracking number '%s' does not match the expected format %s".formatted(value, FORMAT.pattern()));
        }
        value = trimmed;
    }

    public static TrackingNumber of(String value) {
        return new TrackingNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
