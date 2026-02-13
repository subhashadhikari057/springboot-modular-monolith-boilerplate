package com.starterpack.backend.modules.users.api.dto;

import com.starterpack.backend.modules.users.domain.UserStatus;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update user fields")
public record UpdateUserRequest(
        @Schema(example = "Jane Doe")
        @Size(max = 120)
        String name,

        @Schema(example = "+1-415-555-0132")
        @Size(max = 30)
        String phone,

        @Schema(example = "https://cdn.example.com/avatars/jane.png")
        @Size(max = 500)
        String image,

        @Schema(example = "2")
        Integer roleId,

        Boolean emailVerified,
        Boolean phoneVerified,

        UserStatus status
) {
}
