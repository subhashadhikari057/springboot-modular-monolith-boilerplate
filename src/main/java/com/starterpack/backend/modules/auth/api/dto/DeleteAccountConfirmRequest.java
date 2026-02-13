package com.starterpack.backend.modules.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to confirm self account deletion")
public record DeleteAccountConfirmRequest(
        @Schema(description = "Deletion verification token from email", example = "a1b2c3d4...")
        @NotBlank
        @Size(max = 255)
        String token
) {
}
