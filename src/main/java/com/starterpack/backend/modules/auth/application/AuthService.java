package com.starterpack.backend.modules.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.auth.api.dto.ChangePasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.ConfirmVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ForgotPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.LoginRequest;
import com.starterpack.backend.modules.auth.api.dto.RegisterRequest;
import com.starterpack.backend.modules.auth.api.dto.RequestVerificationRequest;
import com.starterpack.backend.modules.auth.api.dto.ResetPasswordRequest;
import com.starterpack.backend.modules.auth.api.dto.UpdateMyProfileRequest;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.Session;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.domain.Verification;
import com.starterpack.backend.modules.users.domain.VerificationChannel;
import com.starterpack.backend.modules.users.domain.VerificationPurpose;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import com.starterpack.backend.modules.users.infrastructure.VerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {
    private static final String LOCAL_PROVIDER = "local";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;
    private final VerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            SessionRepository sessionRepository,
            VerificationRepository verificationRepository,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.verificationRepository = verificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    public AuthSession register(RegisterRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        });

        Role defaultRole = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default role USER not found"));

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(trimToNull(request.phone()));
        user.setImage(trimToNull(request.image()));
        user.setRole(defaultRole);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user = userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setProviderId(LOCAL_PROVIDER);
        account.setAccountId(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        accountRepository.save(account);

        Session session = createSession(user, ipAddress, userAgent);
        return toAuthSession(user, session);
    }

    public AuthSession login(LoginRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        Account account = accountRepository.findByProviderIdAndAccountId(LOCAL_PROVIDER, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (account.getPasswordHash() == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        Session session = createSession(user, ipAddress, userAgent);
        return toAuthSession(user, session);
    }

    public void logout(String sessionToken, String refreshToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            sessionRepository.deleteByToken(sessionToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionRepository.deleteByRefreshToken(refreshToken);
        }
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated"));
    }

    public User updateMyProfile(User currentUser, UpdateMyProfileRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated"));

        user.setName(request.name().trim());
        user.setPhone(trimToNull(request.phone()));
        user.setImage(trimToNull(request.image()));
        return user;
    }

    public AuthSession refreshSessionByRefreshToken(String refreshToken, String ipAddress, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is missing");
        }
        Session existingSession = sessionRepository.findByRefreshTokenAndRefreshExpiresAtAfter(refreshToken, OffsetDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));

        User user = existingSession.getUser();
        sessionRepository.delete(existingSession);
        Session newSession = createSession(user, ipAddress, userAgent);
        return toAuthSession(user, newSession);
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        Account account = accountRepository.findByUserIdAndProviderId(user.getId(), LOCAL_PROVIDER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local account not found"));

        if (account.getPasswordHash() == null || !passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        sessionRepository.deleteByUserId(user.getId());
    }

    public IssuedVerification requestVerification(User user, RequestVerificationRequest request) {
        String target = resolveTarget(user, request.channel(), request.target());
        Verification verification = new Verification();
        verification.setIdentifier(user.getId().toString());
        verification.setTarget(target);
        verification.setPurpose(request.purpose());
        verification.setChannel(request.channel());
        verification.setAttempts(0);
        verification.setMaxAttempts(5);

        String plainToken = generateToken();
        verification.setTokenHash(hashToken(plainToken));
        verification.setExpiresAt(OffsetDateTime.now().plus(authProperties.getVerification().getTtl()));
        verificationRepository.save(verification);
        return new IssuedVerification(verification, plainToken);
    }

    public void confirmVerification(ConfirmVerificationRequest request) {
        Verification verification = resolveVerification(
                request.identifier(),
                request.purpose(),
                request.channel(),
                request.token()
        );

        verification.setConsumedAt(OffsetDateTime.now());
        applyVerificationEffect(verification);
    }

    public Optional<IssuedVerification> forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        Optional<Account> accountOpt = accountRepository.findByProviderIdAndAccountId(LOCAL_PROVIDER, email);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }

        Verification verification = new Verification();
        verification.setIdentifier(user.getId().toString());
        verification.setTarget(email);
        verification.setPurpose(VerificationPurpose.PASSWORD_RESET);
        verification.setChannel(VerificationChannel.EMAIL);
        verification.setAttempts(0);
        verification.setMaxAttempts(5);

        String plainToken = generateToken();
        verification.setTokenHash(hashToken(plainToken));
        verification.setExpiresAt(OffsetDateTime.now().plus(authProperties.getVerification().getTtl()));
        verificationRepository.save(verification);
        return Optional.of(new IssuedVerification(verification, plainToken));
    }

    public void resetPassword(ResetPasswordRequest request) {
        Verification verification = resolveVerification(
                request.identifier(),
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                request.token()
        );

        UUID userId = parseUuid(request.identifier(), "Invalid identifier");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid identifier"));

        Account account = accountRepository.findByUserIdAndProviderId(user.getId(), LOCAL_PROVIDER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local account not found"));

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        verification.setConsumedAt(OffsetDateTime.now());
        sessionRepository.deleteByUserId(user.getId());
    }

    private Session createSession(User user, String ipAddress, String userAgent) {
        Session session = new Session();
        session.setUser(user);
        session.setToken(generateToken());
        session.setExpiresAt(OffsetDateTime.now().plus(authProperties.getSession().getTtl()));
        session.setRefreshToken(generateToken());
        session.setRefreshExpiresAt(OffsetDateTime.now().plus(authProperties.getRefresh().getTtl()));
        session.setIpAddress(trimToNull(ipAddress));
        session.setUserAgent(trimToNull(userAgent));
        return sessionRepository.save(session);
    }

    private AuthSession toAuthSession(User user, Session session) {
        return new AuthSession(
                user,
                session.getToken(),
                session.getExpiresAt(),
                session.getRefreshToken(),
                session.getRefreshExpiresAt()
        );
    }

    private Verification resolveVerification(
            String identifier,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String plainToken
    ) {
        return verificationRepository
                .findFirstByIdentifierAndPurposeAndChannelAndTokenHashAndConsumedAtIsNullAndExpiresAtAfter(
                        identifier,
                        purpose,
                        channel,
                        hashToken(plainToken),
                        OffsetDateTime.now()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
    }

    private void applyVerificationEffect(Verification verification) {
        UUID userId = parseUuid(verification.getIdentifier(), "Invalid verification identifier");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification identifier"));

        if (verification.getPurpose() == VerificationPurpose.EMAIL_VERIFICATION) {
            if (!user.getEmail().equalsIgnoreCase(verification.getTarget())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification target does not match current email");
            }
            user.setEmailVerified(true);
        }

        if (verification.getPurpose() == VerificationPurpose.PHONE_VERIFICATION) {
            if (user.getPhone() == null || !user.getPhone().equals(verification.getTarget())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification target does not match current phone");
            }
            user.setPhoneVerified(true);
        }
    }

    private String resolveTarget(User user, VerificationChannel channel, String target) {
        if (target != null && !target.isBlank()) {
            return target.trim();
        }

        if (channel == VerificationChannel.EMAIL) {
            return user.getEmail();
        }

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone target is required");
        }
        return user.getPhone();
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
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
