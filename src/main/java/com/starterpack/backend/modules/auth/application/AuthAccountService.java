package com.starterpack.backend.modules.auth.application;

import java.util.UUID;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.modules.auth.api.dto.ChangePasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.ResetPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.UpdateMyProfileRequest;
import com.starterpack.backend.modules.auth.application.port.AuthSessionCachePort;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthAccountService {
    private static final String LOCAL_PROVIDER = "local";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionCachePort authSessionCache;
    private final AuthVerificationService authVerificationService;

    public AuthAccountService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            AuthSessionCachePort authSessionCache,
            AuthVerificationService authVerificationService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authSessionCache = authSessionCache;
        this.authVerificationService = authVerificationService;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.unauthorized("Unauthenticated"));
    }

    public User updateMyProfile(User currentUser, UpdateMyProfileRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AppException.unauthorized("Unauthenticated"));

        user.setName(request.name().trim());
        user.setPhone(trimToNull(request.phone()));
        user.setImage(trimToNull(request.image()));
        return user;
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        Account account = accountRepository.findByUserIdAndProviderId(user.getId(), LOCAL_PROVIDER)
                .orElseThrow(() -> AppException.badRequest("Local account not found"));

        if (account.getPasswordHash() == null || !passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw AppException.badRequest("Current password is incorrect");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        sessionRepository.deleteByUserId(user.getId());
        authSessionCache.evictAllUserSessions(user.getId());
    }

    public void resetPassword(ResetPasswordRequest request) {
        UUID userId = authVerificationService.consumePasswordResetToken(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.badRequest("Invalid identifier"));

        Account account = accountRepository.findByUserIdAndProviderId(user.getId(), LOCAL_PROVIDER)
                .orElseThrow(() -> AppException.badRequest("Local account not found"));

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        sessionRepository.deleteByUserId(user.getId());
        authSessionCache.evictAllUserSessions(user.getId());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
