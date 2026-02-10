package com.starterpack.backend.modules.users.api.dto;

import com.starterpack.backend.modules.users.domain.Permission;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Permission summary")
public record PermissionSummary(
        @Schema(example = "12")
        Integer id,

        @Schema(example = "user:write")
        String name
) {
    public static PermissionSummary from(Permission permission) {
        return new PermissionSummary(permission.getId(), permission.getName());
    }
}
