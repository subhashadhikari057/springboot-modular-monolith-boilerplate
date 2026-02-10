package com.starterpack.backend.modules.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to start password reset")
public record ForgotPasswordRequest(
        @Schema(example = "jane.doe@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email
) {
}
