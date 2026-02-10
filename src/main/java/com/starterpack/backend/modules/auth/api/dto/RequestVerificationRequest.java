package com.starterpack.backend.modules.auth.api.dto;

import com.starterpack.backend.modules.users.domain.VerificationChannel;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to issue a verification token")
public record RequestVerificationRequest(
        @Schema(example = "EMAIL_VERIFICATION")
        @NotNull
        VerificationPurpose purpose,

        @Schema(example = "EMAIL")
        @NotNull
        VerificationChannel channel,

        @Schema(description = "Optional target. If omitted, the authenticated user's default target is used.", example = "jane.doe@example.com")
        @Size(max = 255)
        String target
) {
}
