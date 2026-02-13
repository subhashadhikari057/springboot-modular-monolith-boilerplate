package com.starterpack.backend.modules.users.application;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.modules.audit.application.AuditActions;
import com.starterpack.backend.modules.audit.application.AuditEventService;
import com.starterpack.backend.modules.auth.application.port.AuthSessionCachePort;
import com.starterpack.backend.modules.users.api.dto.CreateRoleRequest;
import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.infrastructure.PermissionRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AuthSessionCachePort authSessionCache;
    private final AuditEventService auditEventService;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            SessionRepository sessionRepository,
            AuthSessionCachePort authSessionCache,
            AuditEventService auditEventService
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.authSessionCache = authSessionCache;
        this.auditEventService = auditEventService;
    }

    public Role createRole(CreateRoleRequest request) {
        String name = request.name().trim().toUpperCase(Locale.ROOT);
        roleRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw AppException.conflict("Role already exists");
        });

        Role role = new Role();
        role.setName(name);
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);
        auditEventService.record(AuditEventService.AuditEvent.success(
                AuditActions.ROLES_CREATE,
                "role",
                saved.getId().toString(),
                java.util.Map.of("name", saved.getName())
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    public Role updateRolePermissions(Integer roleId, Set<Integer> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> AppException.notFound("Role not found"));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw AppException.notFound("One or more permissions not found");
        }

        role.setPermissions(new HashSet<>(permissions));
        revokeRoleSessions(roleId);
        auditEventService.record(AuditEventService.AuditEvent.success(
                AuditActions.ROLES_PERMISSIONS_UPDATE,
                "role",
                role.getId().toString(),
                java.util.Map.of("permissionCount", permissions.size())
        ));
        return role;
    }

    private void revokeRoleSessions(Integer roleId) {
        List<UUID> userIds = userRepository.findIdsByRoleId(roleId);
        sessionRepository.deleteByUserRoleId(roleId);
        userIds.forEach(authSessionCache::evictAllUserSessions);
    }
}
