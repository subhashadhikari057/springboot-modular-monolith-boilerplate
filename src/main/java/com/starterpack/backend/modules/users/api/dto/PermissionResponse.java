package com.starterpack.backend.modules.users.api.dto;

import java.time.OffsetDateTime;

import com.starterpack.backend.modules.users.domain.Permission;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Permission response")
public record PermissionResponse(
        @Schema(example = "12")
        Integer id,

        @Schema(example = "user:write")
        String name,

        @Schema(example = "Create and update users")
        String description,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getDescription(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}
