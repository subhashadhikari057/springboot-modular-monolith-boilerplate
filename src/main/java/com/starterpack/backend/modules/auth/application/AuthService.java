package com.starterpack.backend.modules.auth.application;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.auth.api.dto.ChangePasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.ConfirmVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ForgotPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.LoginRequest;
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

    public IssuedVerification requestVerification(User user, RequestVerificationRequest request) {
        return toIssuedVerification(authVerificationService.requestVerification(user, request));
    }

    public void confirmVerification(ConfirmVerificationRequest request) {
        authVerificationService.confirmVerification(request);
    }

    public Optional<IssuedVerification> forgotPassword(ForgotPasswordRequest request) {
        return authVerificationService.forgotPassword(request).map(this::toIssuedVerification);
    }

    public void resetPassword(ResetPasswordRequest request) {
        authAccountService.resetPassword(request);
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
}
