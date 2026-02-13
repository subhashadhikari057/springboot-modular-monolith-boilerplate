package com.starterpack.backend.modules.auth.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.auth.api.dto.ChangePasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.ConfirmVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.DeleteAccountConfirmRequest;
import com.starterpack.backend.modules.auth.api.dto.ForgotPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.LoginRequest;
import com.starterpack.backend.modules.auth.api.dto.ReauthRequest;
import com.starterpack.backend.modules.auth.api.dto.RegisterRequest;
import com.starterpack.backend.modules.auth.api.dto.RequestVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ResetPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.UpdateMyProfileRequest;
import com.starterpack.backend.modules.auth.application.AuthAuthenticationService.AuthSessionData;
import com.starterpack.backend.modules.auth.application.AuthVerificationService.IssuedVerificationData;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthAuthenticationService authAuthenticationService;
    private final AuthAccountService authAccountService;
    private final AuthVerificationService authVerificationService;

    public AuthService(
            AuthAuthenticationService authAuthenticationService,
            AuthAccountService authAccountService,
            AuthVerificationService authVerificationService
    ) {
        this.authAuthenticationService = authAuthenticationService;
        this.authAccountService = authAccountService;
        this.authVerificationService = authVerificationService;
    }

    public AuthSession register(RegisterRequest request, String ipAddress, String userAgent) {
        return toAuthSession(authAuthenticationService.register(request, ipAddress, userAgent));
    }

    public AuthSession login(LoginRequest request, String ipAddress, String userAgent) {
        return toAuthSession(authAuthenticationService.login(request, ipAddress, userAgent));
    }

    public void logout(String sessionToken, String refreshToken) {
        authAuthenticationService.logout(sessionToken, refreshToken);
    }

    public List<ManagedSession> listSessions(UUID userId, String currentSessionToken) {
        return authAuthenticationService.listSessions(userId, currentSessionToken).stream()
                .map(this::toManagedSession)
                .toList();
    }

    public boolean revokeSession(UUID userId, UUID sessionId, String currentSessionToken) {
        return authAuthenticationService.revokeSession(userId, sessionId, currentSessionToken);
    }

    public void logoutAll(UUID userId) {
        authAuthenticationService.logoutAll(userId);
    }

    public User getCurrentUser(UUID userId) {
        return authAccountService.getCurrentUser(userId);
    }

    public User updateMyProfile(User currentUser, UpdateMyProfileRequest request) {
        return authAccountService.updateMyProfile(currentUser, request);
    }

    public AuthSession refreshSessionByRefreshToken(String refreshToken, String ipAddress, String userAgent) {
        return toAuthSession(authAuthenticationService.refreshSessionByRefreshToken(refreshToken, ipAddress, userAgent));
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        authAccountService.changePassword(user, request);
    }

    public void reauthenticate(User user, ReauthRequest request) {
        authAuthenticationService.reauthenticate(user, request.password());
    }

    public IssuedVerification requestVerification(User user, RequestVerificationRequest request) {
        return toIssuedVerification(authVerificationService.requestVerification(user, request));
    }

    public IssuedVerification resendVerification(User user, RequestVerificationRequest request) {
        return toIssuedVerification(authVerificationService.resendVerification(user, request));
    }

    public void confirmVerification(ConfirmVerificationRequest request) {
        authVerificationService.confirmVerification(request);
    }

    public Optional<IssuedVerification> forgotPassword(ForgotPasswordRequest request) {
        return authVerificationService.forgotPassword(request).map(this::toIssuedVerification);
    }

    public Optional<IssuedVerification> requestPasswordResetByEmail(String email) {
        return authVerificationService.forgotPasswordByEmail(email).map(this::toIssuedVerification);
    }

    public void resetPassword(ResetPasswordRequest request) {
        authAccountService.resetPassword(request);
    }

    public IssuedVerification requestAccountDeletionVerification(User user) {
        return toIssuedVerification(authVerificationService.requestAccountDeletionVerification(user));
    }

    public void deleteMyAccount(User user, DeleteAccountConfirmRequest request) {
        authAccountService.deleteMyAccount(user, request);
    }

    private AuthSession toAuthSession(AuthSessionData sessionData) {
        return new AuthSession(
                sessionData.user(),
                sessionData.token(),
                sessionData.expiresAt(),
                sessionData.refreshToken(),
                sessionData.refreshExpiresAt()
        );
    }

    private IssuedVerification toIssuedVerification(IssuedVerificationData issued) {
        return new IssuedVerification(issued.verification(), issued.token());
    }

    private ManagedSession toManagedSession(AuthAuthenticationService.ManagedSessionData session) {
        return new ManagedSession(
                session.sessionId(),
                session.current(),
                session.createdAt(),
                session.expiresAt(),
                session.refreshExpiresAt(),
                session.ipAddress(),
                session.userAgent()
        );
    }

    public record AuthSession(
            User user,
            String token,
            OffsetDateTime expiresAt,
            String refreshToken,
            OffsetDateTime refreshExpiresAt
    ) {
    }

    public record IssuedVerification(Verification verification, String token) {
    }

    public record ManagedSession(
            UUID sessionId,
            boolean current,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt,
            OffsetDateTime refreshExpiresAt,
            String ipAddress,
            String userAgent
    ) {
    }
}
