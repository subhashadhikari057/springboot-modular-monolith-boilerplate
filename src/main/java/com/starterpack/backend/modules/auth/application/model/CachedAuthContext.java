package com.starterpack.backend.modules.auth.application.model;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.Session;

public record CachedAuthContext(
        UUID sessionId,
        UUID userId,
        String sessionToken,
        OffsetDateTime expiresAt,
        String refreshToken,
        OffsetDateTime refreshExpiresAt,
        String roleName,
        Set<String> permissions
) {
    public static CachedAuthContext fromSession(Session session) {
        Role role = session.getUser().getRole();
        Set<String> permissionNames = role == null
                ? Set.of()
                : role.getPermissions().stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet());
        return new CachedAuthContext(
                session.getId(),
                session.getUser().getId(),
                session.getToken(),
                session.getExpiresAt(),
                session.getRefreshToken(),
                session.getRefreshExpiresAt(),
                role == null ? null : role.getName(),
                permissionNames
        );
    }
}
