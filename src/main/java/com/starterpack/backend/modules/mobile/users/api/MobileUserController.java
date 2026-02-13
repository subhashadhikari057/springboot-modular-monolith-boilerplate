package com.starterpack.backend.modules.mobile.users.api;

import com.starterpack.backend.modules.users.api.dto.UserPermissionsResponse;
import com.starterpack.backend.modules.users.application.UserService;
import com.starterpack.backend.modules.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mobile/users")
@Tag(name = "Users", description = "User self endpoints")
@Validated
public class MobileUserController {
    private final UserService userService;

    public MobileUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get my permissions", description = "Returns effective permissions for current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserPermissionsResponse.class)))
    })
    @GetMapping("/me/permissions")
    public UserPermissionsResponse getMyPermissions(Authentication authentication) {
        User user = currentUser(authentication);
        return userService.getUserPermissions(user.getId());
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        return user;
    }
}
