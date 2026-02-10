package com.starterpack.backend.modules.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to complete password reset")
public record ResetPasswordRequest(
        @Schema(description = "Identifier returned by forgot-password flow", example = "6cfb19a7-71a3-46a8-b1d8-3de77bcd9b61")
        @NotBlank
        String identifier,

        @Schema(description = "Verification token returned by forgot-password flow", example = "f3f9a2fef8784635b71f8549f6d8f862")
        @NotBlank
        String token,

        @Schema(example = "NewP@ssword2")
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
