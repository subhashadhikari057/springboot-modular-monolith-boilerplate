package com.starterpack.backend.modules.auth.api.dto;

import com.starterpack.backend.modules.users.domain.VerificationChannel;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to confirm a verification token")
public record ConfirmVerificationRequest(
        @Schema(description = "Identifier returned by verification request", example = "6cfb19a7-71a3-46a8-b1d8-3de77bcd9b61")
        @NotBlank
        String identifier,

        @Schema(example = "EMAIL_VERIFICATION")
        @NotNull
        VerificationPurpose purpose,

        @Schema(example = "EMAIL")
        @NotNull
        VerificationChannel channel,

        @Schema(example = "f3f9a2fef8784635b71f8549f6d8f862")
        @NotBlank
        String token
) {
}
