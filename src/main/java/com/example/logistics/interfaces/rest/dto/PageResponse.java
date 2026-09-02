package com.example.logistics.interfaces.rest.dto;

import java.util.List;

/**
 * Paginated listing envelope returned by {@code GET /api/v1/shipments}.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements,
                                         int totalPages, boolean hasNext) {
        return new PageResponse<>(List.copyOf(content), page, size, totalElements, totalPages, hasNext);
    }
}
