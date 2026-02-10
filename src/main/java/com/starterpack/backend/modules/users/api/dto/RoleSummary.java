package com.starterpack.backend.modules.users.api.dto;

import com.starterpack.backend.modules.users.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Role summary")
public record RoleSummary(
        @Schema(example = "3")
        Integer id,

        @Schema(example = "USER")
        String name
) {
    public static RoleSummary from(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleSummary(role.getId(), role.getName());
    }
}
