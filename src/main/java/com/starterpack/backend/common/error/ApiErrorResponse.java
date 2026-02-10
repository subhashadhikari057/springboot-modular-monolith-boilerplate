package com.starterpack.backend.common.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details
) {
    public static ApiErrorResponse of(
            int status,
            String error,
            String code,
            String message,
            String path,
            List<String> details
    ) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status,
                error,
                code,
                message,
                path,
                details == null ? List.of() : details
        );
    }
}
