package com.starterpack.backend.modules.auth.api.dto;

import java.time.OffsetDateTime;

import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationChannel;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued verification details. Token is returned for local development/testing.")
public record VerificationIssuedResponse(
        String identifier,
        String target,
        VerificationPurpose purpose,
        VerificationChannel channel,
        OffsetDateTime expiresAt,
        @Schema(description = "Plain token for testing. Do not expose in production logs/UIs.")
        String token
) {
    public static VerificationIssuedResponse from(Verification verification, String token) {
        return new VerificationIssuedResponse(
                verification.getIdentifier(),
                verification.getTarget(),
                verification.getPurpose(),
                verification.getChannel(),
                verification.getExpiresAt(),
                token
        );
    }
}
