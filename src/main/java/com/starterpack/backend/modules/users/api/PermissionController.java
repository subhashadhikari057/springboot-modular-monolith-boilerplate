package com.starterpack.backend.modules.users.api;

import java.util.List;

import com.starterpack.backend.modules.users.api.dto.CreatePermissionRequest;
import com.starterpack.backend.modules.users.api.dto.PermissionResponse;
import com.starterpack.backend.modules.users.application.PermissionService;
import com.starterpack.backend.modules.users.domain.Permission;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Permissions", description = "Permission catalog")
@Validated
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Create permission", description = "Creates a new permission.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Permission created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PermissionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Permission already exists", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        Permission permission = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PermissionResponse.from(permission));
    }

    @Operation(summary = "List permissions", description = "Returns all permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions list",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PermissionResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public List<PermissionResponse> listPermissions() {
        return permissionService.listPermissions().stream().map(PermissionResponse::from).toList();
    }
}
