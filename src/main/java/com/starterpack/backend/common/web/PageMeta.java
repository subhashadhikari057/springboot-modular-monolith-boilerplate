package com.starterpack.backend.common.web;

import org.springframework.data.domain.Page;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static PageMeta from(Page<?> page) {
        return new PageMeta(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
