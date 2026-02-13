package com.starterpack.backend.modules.users.api.dto;

import com.starterpack.backend.modules.users.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update user account status")
public record UpdateUserStatusRequest(
        @NotNull
        UserStatus status
) {
}
