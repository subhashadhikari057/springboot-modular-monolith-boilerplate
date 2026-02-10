package com.starterpack.backend.modules.users.application;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.starterpack.backend.modules.users.api.dto.CreateRoleRequest;
import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.infrastructure.PermissionRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public Role createRole(CreateRoleRequest request) {
        String name = request.name().trim().toUpperCase(Locale.ROOT);
        roleRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        });

        Role role = new Role();
        role.setName(name);
        role.setDescription(request.description());
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    public Role updateRolePermissions(Integer roleId, Set<Integer> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more permissions not found");
        }

        role.setPermissions(new HashSet<>(permissions));
        return role;
    }
}
