package com.starterpack.backend.modules.users.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a permission")
public record CreatePermissionRequest(
        @Schema(example = "users:update")
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(example = "Create and update users")
        @Size(max = 255)
        String description
) {
}
