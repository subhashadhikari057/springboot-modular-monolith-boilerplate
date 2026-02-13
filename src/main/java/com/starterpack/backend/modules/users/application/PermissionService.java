package com.starterpack.backend.modules.users.application;

import java.util.List;
import java.util.Locale;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.modules.audit.application.AuditActions;
import com.starterpack.backend.modules.audit.application.AuditEventService;
import com.starterpack.backend.modules.users.api.dto.CreatePermissionRequest;
import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.infrastructure.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final AuditEventService auditEventService;

    public PermissionService(PermissionRepository permissionRepository, AuditEventService auditEventService) {
        this.permissionRepository = permissionRepository;
        this.auditEventService = auditEventService;
    }

    public Permission createPermission(CreatePermissionRequest request) {
        String name = request.name().trim().toLowerCase(Locale.ROOT);
        permissionRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw AppException.conflict("Permission already exists");
        });

        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(request.description());
        Permission saved = permissionRepository.save(permission);
        auditEventService.record(AuditEventService.AuditEvent.success(
                AuditActions.PERMISSIONS_CREATE,
                "permission",
                saved.getId().toString(),
                java.util.Map.of("name", saved.getName())
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }
}
