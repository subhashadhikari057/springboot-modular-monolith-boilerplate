package com.starterpack.backend.modules.users.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User response")
public record UserResponse(
        @Schema(example = "6cfb19a7-71a3-46a8-b1d8-3de77bcd9b61")
        UUID id,

        @Schema(example = "Jane Doe")
        String name,

        @Schema(example = "jane.doe@example.com")
        String email,

        @Schema(example = "false")
        boolean emailVerified,

        @Schema(example = "+1-415-555-0132")
        String phone,

        @Schema(example = "false")
        boolean phoneVerified,

        @Schema(example = "https://cdn.example.com/avatars/jane.png")
        String image,

        UserStatus status,

        RoleSummary role,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getImage(),
                user.getStatus(),
                RoleSummary.from(user.getRole()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
