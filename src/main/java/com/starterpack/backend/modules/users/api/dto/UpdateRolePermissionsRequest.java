package com.starterpack.backend.modules.users.api.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to replace role permissions")
public record UpdateRolePermissionsRequest(
        @Schema(example = "[1, 2, 3]")
        @NotEmpty
        Set<Integer> permissionIds
) {
}
