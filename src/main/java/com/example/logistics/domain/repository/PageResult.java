package com.example.logistics.domain.repository;

import java.util.List;

/**
 * A simple, framework-agnostic pagination result. Keeps Spring's {@code Page}
 * types out of the domain layer.
 *
 * @param <T> the type of elements in the page
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult {
        content = List.copyOf(content);
    }

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        final int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }
}
