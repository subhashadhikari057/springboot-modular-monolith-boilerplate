package com.starterpack.backend.modules.auth.api.dto;

import java.time.OffsetDateTime;

import com.starterpack.backend.modules.users.api.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response")
public record AuthResponse(
        UserResponse user,

        @Schema(description = "Access token expiration time")
        OffsetDateTime expiresAt,

        @Schema(description = "Refresh token expiration time")
        OffsetDateTime refreshExpiresAt
) {
}
