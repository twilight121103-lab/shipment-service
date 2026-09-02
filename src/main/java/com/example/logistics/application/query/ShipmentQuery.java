package com.example.logistics.application.query;

import com.example.logistics.domain.model.ShipmentStatus;

/**
 * Immutable search criteria for listing shipments.
 */
public record ShipmentQuery(
        ShipmentStatus status,
        String trackingNumberLike,
        int page,
        int size,
        String sortBy,
        SortDirection sortDirection) {

    public enum SortDirection {
        ASC, DESC;

        public static SortDirection parse(String value, SortDirection fallback) {
            if (value == null) {
                return fallback;
            }
            try {
                return valueOf(value.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }

    public static ShipmentQuery of(ShipmentStatus status, String trackingNumberLike, int page, int size,
                                   String sortBy, SortDirection sortDirection) {
        return new ShipmentQuery(
                status,
                blankToNull(trackingNumberLike),
                Math.max(0, page),
                Math.max(1, Math.min(size, 200)),
                sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim(),
                sortDirection == null ? SortDirection.DESC : sortDirection);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
