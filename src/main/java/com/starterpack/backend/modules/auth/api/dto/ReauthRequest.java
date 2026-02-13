package com.starterpack.backend.modules.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to re-authenticate current user")
public record ReauthRequest(
        @Schema(example = "Str0ngP@ssword")
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
