package com.starterpack.backend.modules.users.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.starterpack.backend.modules.users.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Role response")
public record RoleResponse(
        @Schema(example = "2")
        Integer id,

        @Schema(example = "ADMIN")
        String name,

        @Schema(example = "Administrative access to manage users")
        String description,

        List<PermissionSummary> permissions,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static RoleResponse from(Role role) {
        List<PermissionSummary> permissionSummaries = role.getPermissions().stream()
                .map(PermissionSummary::from)
                .toList();

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                permissionSummaries,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
