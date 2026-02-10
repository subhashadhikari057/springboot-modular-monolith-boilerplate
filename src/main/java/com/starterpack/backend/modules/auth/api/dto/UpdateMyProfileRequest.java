package com.starterpack.backend.modules.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update the current user's profile")
public record UpdateMyProfileRequest(
        @Schema(example = "Super Admin")
        @NotBlank
        @Size(max = 120)
        String name,

        @Schema(example = "+1-415-555-0132")
        @Size(max = 30)
        String phone,

        @Schema(example = "https://cdn.example.com/avatars/superadmin.png")
        @Size(max = 500)
        String image
) {
}
