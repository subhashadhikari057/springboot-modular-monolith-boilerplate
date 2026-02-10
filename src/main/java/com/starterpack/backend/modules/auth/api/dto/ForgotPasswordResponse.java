package com.starterpack.backend.modules.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Forgot-password response. Token fields are for local development/testing.")
public record ForgotPasswordResponse(
        String message,
        String identifier,
        String token
) {
    public static ForgotPasswordResponse generic() {
        return new ForgotPasswordResponse("If the account exists, reset instructions have been issued", null, null);
    }
}
