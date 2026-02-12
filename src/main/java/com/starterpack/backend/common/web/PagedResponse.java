package com.starterpack.backend.common.web;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        PageMeta page
) {
}
