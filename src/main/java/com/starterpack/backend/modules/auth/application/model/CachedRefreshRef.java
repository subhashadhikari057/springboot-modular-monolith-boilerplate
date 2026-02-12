package com.starterpack.backend.modules.auth.application.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CachedRefreshRef(
        UUID sessionId,
        UUID userId,
        String sessionToken,
        String refreshToken,
        OffsetDateTime refreshExpiresAt
) {
}
