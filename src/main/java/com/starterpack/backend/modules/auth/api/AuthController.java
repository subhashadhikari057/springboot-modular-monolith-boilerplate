package com.starterpack.backend.modules.auth.api;

import java.util.Arrays;

import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.auth.api.dto.AuthResponse;
import com.starterpack.backend.modules.auth.api.dto.ChangePasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.ConfirmVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ForgotPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.ForgotPasswordResponse;
import com.starterpack.backend.modules.auth.api.dto.LoginRequest;
import com.starterpack.backend.modules.auth.api.dto.MessageResponse;
import com.starterpack.backend.modules.auth.api.dto.RegisterRequest;
import com.starterpack.backend.modules.auth.api.dto.RequestVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ResetPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.UpdateMyProfileRequest;
import com.starterpack.backend.modules.auth.api.dto.VerificationIssuedResponse;
import com.starterpack.backend.modules.auth.application.AuthCookieService;
import com.starterpack.backend.modules.auth.application.AuthService;
import com.starterpack.backend.modules.auth.application.AuthService.AuthSession;
import com.starterpack.backend.modules.users.api.dto.UserResponse;
import com.starterpack.backend.modules.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication, sessions, verification, and password recovery")
@Validated
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthCookieService authCookieService, AuthProperties authProperties) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.authProperties = authProperties;
    }

    @Operation(summary = "Register", description = "Creates a local account and starts an authenticated session.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        AuthSession session = authService.register(request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
        return withSessionCookie(session, HttpStatus.CREATED);
    }

    @Operation(summary = "Login", description = "Authenticates with email/password and sets session cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthSession session = authService.login(request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
        return withSessionCookie(session, HttpStatus.OK);
    }

    @Operation(summary = "Logout", description = "Ends current session and clears session cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest servletRequest) {
        authService.logout(extractSessionToken(servletRequest), extractRefreshToken(servletRequest));
        HttpHeaders headers = new HttpHeaders();
        authCookieService.clearSessionCookie(headers);
        authCookieService.clearRefreshCookie(headers);
        return ResponseEntity.ok().headers(headers).body(new MessageResponse("logged_out"));
    }

    @Operation(summary = "Current user", description = "Returns the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content)
    })
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = currentUser(authentication);
        return UserResponse.from(authService.getCurrentUser(user.getId()));
    }

    @Operation(summary = "Update current profile", description = "Updates the authenticated user's profile fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content),
            @ApiResponse(responseCode = "409", description = "Phone already in use", content = @Content)
    })
    @PatchMapping("/me")
    public UserResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        User user = currentUser(authentication);
        return UserResponse.from(authService.updateMyProfile(user, request));
    }

    @Operation(summary = "Refresh session", description = "Rotates session token and updates cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session refreshed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest servletRequest) {
        AuthSession session = authService.refreshSessionByRefreshToken(
                extractRefreshToken(servletRequest),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
        return withSessionCookie(session, HttpStatus.OK);
    }

    @Operation(summary = "Change password", description = "Changes password for the current user and logs out all sessions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid password", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content)
    })
    @PostMapping("/password/change")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = currentUser(authentication);
        authService.changePassword(user, request);
        HttpHeaders headers = new HttpHeaders();
        authCookieService.clearSessionCookie(headers);
        authCookieService.clearRefreshCookie(headers);
        return ResponseEntity.ok().headers(headers).body(new MessageResponse("password_changed"));
    }

    @Operation(summary = "Request verification", description = "Creates a verification token for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification issued",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VerificationIssuedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content)
    })
    @PostMapping("/verify/request")
    public VerificationIssuedResponse requestVerification(
            Authentication authentication,
            @Valid @RequestBody RequestVerificationRequest request
    ) {
        User user = currentUser(authentication);
        AuthService.IssuedVerification issued = authService.requestVerification(user, request);
        String token = authProperties.getVerification().isExposeTokenInResponse() ? issued.token() : null;
        return VerificationIssuedResponse.from(issued.verification(), token);
    }

    @Operation(summary = "Confirm verification", description = "Confirms email/phone/password-reset verification token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification confirmed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content)
    })
    @PostMapping("/verify/confirm")
    public MessageResponse confirmVerification(@Valid @RequestBody ConfirmVerificationRequest request) {
        authService.confirmVerification(request);
        return new MessageResponse("verified");
    }

    @Operation(summary = "Forgot password", description = "Creates password reset verification for local account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request accepted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForgotPasswordResponse.class)))
    })
    @PostMapping("/password/forgot")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request)
                .map(issued -> new ForgotPasswordResponse(
                        "If the account exists, reset instructions have been issued",
                        issued.verification().getIdentifier(),
                        authProperties.getVerification().isExposeTokenInResponse() ? issued.token() : null
                ))
                .orElseGet(ForgotPasswordResponse::generic);
    }

    @Operation(summary = "Reset password", description = "Resets local account password using verification token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content)
    })
    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("password_reset");
    }

    private ResponseEntity<AuthResponse> withSessionCookie(AuthSession session, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        authCookieService.writeSessionCookie(headers, session.token(), authProperties.getSession().getTtl());
        authCookieService.writeRefreshCookie(headers, session.refreshToken(), authProperties.getRefresh().getTtl());
        AuthResponse body = new AuthResponse(UserResponse.from(session.user()), session.expiresAt(), session.refreshExpiresAt());
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    private String extractSessionToken(HttpServletRequest request) {
        return extractCookieValue(request, authProperties.getCookie().getName());
    }

    private String extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, authProperties.getRefreshCookie().getName());
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        return user;
    }
}
