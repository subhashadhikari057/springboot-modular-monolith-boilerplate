package com.starterpack.backend.modules.users.api.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Effective user permissions response")
public record UserPermissionsResponse(
        UUID userId,
        RoleSummary role,
        List<String> permissions
) {
}
