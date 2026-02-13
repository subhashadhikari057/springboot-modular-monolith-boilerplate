package com.starterpack.backend.modules.users.api;

import java.net.URI;
import java.util.UUID;

import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.modules.auth.api.dto.MessageResponse;
import com.starterpack.backend.modules.users.api.dto.CreateUserRequest;
import com.starterpack.backend.modules.users.api.dto.UpdateUserRequest;
import com.starterpack.backend.modules.users.api.dto.UpdateUserStatusRequest;
import com.starterpack.backend.modules.users.api.dto.UpdateUserRoleRequest;
import com.starterpack.backend.modules.users.api.dto.UserPermissionsResponse;
import com.starterpack.backend.modules.users.api.dto.UserResponse;
import com.starterpack.backend.modules.users.application.UserService;
import com.starterpack.backend.modules.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management, listing, and role assignment")
@Validated
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create user", description = "Creates a new user and assigns a role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        URI location = URI.create("/api/users/" + user.getId());
        return ResponseEntity.created(location).body(UserResponse.from(user));
    }

    @Operation(summary = "Get user", description = "Returns a single user by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public UserResponse getUser(
            @Parameter(description = "User id", example = "6cfb19a7-71a3-46a8-b1d8-3de77bcd9b61")
            @PathVariable UUID id
    ) {
        return UserResponse.from(userService.getUser(id));
    }

    @Operation(summary = "List users", description = "Returns all users.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users list",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public PagedResponse<UserResponse> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Boolean emailVerified
    ) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
        }

        String resolvedSortBy = resolveSortBy(sortBy);
        Sort.Direction direction = parseDirection(sortDir);
        return userService.listUsers(page - 1, size, resolvedSortBy, direction, q, roleId, emailVerified);
    }

    private String resolveSortBy(String sortBy) {
        return switch (sortBy) {
            case "id", "name", "email", "createdAt", "updatedAt" -> sortBy;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sortBy. Allowed: id,name,email,createdAt,updatedAt"
            );
        };
    }

    private Sort.Direction parseDirection(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortDir must be asc or desc");
        }
    }

    @Operation(summary = "Update user role", description = "Assigns a new role to a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
    })
    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('user:update-role')")
    public UserResponse updateUserRole(
            @Parameter(description = "User id", example = "6cfb19a7-71a3-46a8-b1d8-3de77bcd9b61")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return UserResponse.from(userService.updateUserRole(id, request.roleId()));
    }

    @Operation(summary = "Update user", description = "Partially updates user fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public UserResponse updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return UserResponse.from(userService.updateUser(id, request));
    }

    @Operation(summary = "Update user status", description = "Sets account status (ACTIVE, DISABLED, LOCKED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:update-status')")
    public UserResponse updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return UserResponse.from(userService.updateUserStatus(id, request.status()));
    }

    @Operation(summary = "Get user permissions", description = "Returns effective permissions for a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserPermissionsResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('user:read-permissions')")
    public UserPermissionsResponse getUserPermissions(@PathVariable UUID id) {
        return userService.getUserPermissions(id);
    }

    @Operation(summary = "Get my permissions", description = "Returns effective permissions for current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserPermissionsResponse.class)))
    })
    @GetMapping("/me/permissions")
    @PreAuthorize("hasAuthority('user:read')")
    public UserPermissionsResponse getMyPermissions(Authentication authentication) {
        User user = currentUser(authentication);
        return userService.getUserPermissions(user.getId());
    }

    @Operation(summary = "Request user password reset", description = "Triggers password reset email for the target user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request accepted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/{id}/password/reset/request")
    @PreAuthorize("hasAuthority('user:reset-password-request')")
    public MessageResponse requestUserPasswordReset(@PathVariable UUID id) {
        userService.requestPasswordResetForUser(id);
        return new MessageResponse("password_reset_requested");
    }

    @Operation(summary = "Delete user", description = "Deletes a user and related auth records.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User id", example = "6cfb19a7-71a3-46a8-b1d8-3de77bcd9b61")
            @PathVariable UUID id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        return user;
    }
}
