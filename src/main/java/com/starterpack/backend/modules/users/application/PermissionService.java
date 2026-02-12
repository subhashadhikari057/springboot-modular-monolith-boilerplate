package com.starterpack.backend.modules.users.application;

import java.util.List;
import java.util.Locale;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.modules.users.api.dto.CreatePermissionRequest;
import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.infrastructure.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public Permission createPermission(CreatePermissionRequest request) {
        String name = request.name().trim().toLowerCase(Locale.ROOT);
        permissionRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw AppException.conflict("Permission already exists");
        });

        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(request.description());
        return permissionRepository.save(permission);
    }

    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }
}
