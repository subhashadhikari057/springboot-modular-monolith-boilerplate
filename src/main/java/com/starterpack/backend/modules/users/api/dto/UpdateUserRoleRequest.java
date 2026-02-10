package com.starterpack.backend.modules.users.api.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update a user's role")
public record UpdateUserRoleRequest(
        @Schema(example = "2")
        @NotNull
        Integer roleId
) {
}
