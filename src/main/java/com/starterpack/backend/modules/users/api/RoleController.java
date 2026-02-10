package com.starterpack.backend.modules.users.api;

import java.util.List;

import com.starterpack.backend.modules.users.api.dto.CreateRoleRequest;
import com.starterpack.backend.modules.users.api.dto.RoleResponse;
import com.starterpack.backend.modules.users.api.dto.UpdateRolePermissionsRequest;
import com.starterpack.backend.modules.users.application.RoleService;
import com.starterpack.backend.modules.users.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Role management")
@Validated
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Create role", description = "Creates a new role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "409", description = "Role already exists", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(role));
    }

    @Operation(summary = "List roles", description = "Returns all roles with their permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles list",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoleResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public List<RoleResponse> listRoles() {
        return roleService.listRoles().stream().map(RoleResponse::from).toList();
    }

    @Operation(summary = "Update role permissions", description = "Replaces the permissions assigned to a role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role or permission not found", content = @Content)
    })
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:update-permissions')")
    public RoleResponse updateRolePermissions(
            @Parameter(description = "Role id", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        return RoleResponse.from(roleService.updateRolePermissions(id, request.permissionIds()));
    }
}
