package com.starterpack.backend.modules.auth.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Active session/device details")
public record AuthSessionInfoResponse(
        UUID sessionId,
        boolean current,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime refreshExpiresAt,
        String ipAddress,
        String userAgent
) {
}
