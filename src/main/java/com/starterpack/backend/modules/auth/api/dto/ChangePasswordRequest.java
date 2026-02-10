package com.starterpack.backend.modules.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to change the current user's password")
public record ChangePasswordRequest(
        @Schema(example = "OldP@ssword1")
        @NotBlank
        @Size(min = 8, max = 72)
        String currentPassword,

        @Schema(example = "NewP@ssword2")
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
