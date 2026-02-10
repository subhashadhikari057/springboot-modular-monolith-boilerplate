package com.starterpack.backend.modules.users.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a role")
public record CreateRoleRequest(
        @Schema(example = "ADMIN")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(example = "Administrative access to manage users")
        @Size(max = 255)
        String description
) {
}
