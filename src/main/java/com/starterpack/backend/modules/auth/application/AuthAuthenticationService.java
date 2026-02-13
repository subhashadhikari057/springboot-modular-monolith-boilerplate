package com.starterpack.backend.modules.auth.application;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.audit.application.AuditActions;
import com.starterpack.backend.modules.audit.application.AuditEventService;
import com.starterpack.backend.modules.auth.api.dto.LoginRequest;
import com.starterpack.backend.modules.auth.api.dto.RegisterRequest;
import com.starterpack.backend.modules.auth.application.port.AuthSessionCachePort;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.Session;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthAuthenticationService {
    private static final String LOCAL_PROVIDER = "local";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final AuthSessionCachePort authSessionCache;
    private final AuthTokenService authTokenService;
    private final AuditEventService auditEventService;

    public AuthAuthenticationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties,
            AuthSessionCachePort authSessionCache,
            AuthTokenService authTokenService,
            AuditEventService auditEventService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.authSessionCache = authSessionCache;
        this.authTokenService = authTokenService;
        this.auditEventService = auditEventService;
    }

    public AuthSessionData register(RegisterRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        try {
            userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
                throw AppException.conflict("Email already in use");
            });

            Role defaultRole = roleRepository.findByNameIgnoreCase("USER")
                    .orElseThrow(() -> AppException.badRequest("Default role USER not found"));

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
            authSessionCache.cacheSession(session);
            auditEventService.record(AuditEventService.AuditEvent.success(
                    AuditActions.AUTH_REGISTER_SUCCESS,
                    "session",
                    session.getId().toString(),
                    Map.of("provider", LOCAL_PROVIDER)
            ));
            return toAuthSessionData(user, session);
        } catch (AppException ex) {
            auditEventService.record(AuditEventService.AuditEvent.failure(
                    AuditActions.AUTH_REGISTER_FAILURE,
                    "auth",
                    null,
                    ex.getCode(),
                    email,
                    ipAddress,
                    userAgent,
                    Map.of("provider", LOCAL_PROVIDER)
            ));
            throw ex;
        }
    }

    public AuthSessionData login(LoginRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        try {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> AppException.unauthorized("Invalid credentials"));

            Account account = accountRepository.findByProviderIdAndAccountId(LOCAL_PROVIDER, email)
                    .orElseThrow(() -> AppException.unauthorized("Invalid credentials"));

            if (account.getPasswordHash() == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
                throw AppException.unauthorized("Invalid credentials");
            }

            Session session = createSession(user, ipAddress, userAgent);
            authSessionCache.cacheSession(session);
            auditEventService.record(AuditEventService.AuditEvent.success(
                    AuditActions.AUTH_LOGIN_SUCCESS,
                    "session",
                    session.getId().toString(),
                    Map.of("provider", LOCAL_PROVIDER)
            ));
            return toAuthSessionData(user, session);
        } catch (AppException ex) {
            auditEventService.record(AuditEventService.AuditEvent.failure(
                    AuditActions.AUTH_LOGIN_FAILURE,
                    "auth",
                    null,
                    ex.getCode(),
                    email,
                    ipAddress,
                    userAgent,
                    Map.of("provider", LOCAL_PROVIDER)
            ));
            throw ex;
        }
    }

    public void logout(String sessionToken, String refreshToken) {
        Optional<Session> session = resolveSessionForLogout(sessionToken, refreshToken);
        session.ifPresentOrElse(existing -> {
                    sessionRepository.delete(existing);
                    authSessionCache.evictSession(existing.getToken(), existing.getRefreshToken(), existing.getUser().getId());
                    auditEventService.record(AuditEventService.AuditEvent.success(
                            AuditActions.AUTH_LOGOUT,
                            "session",
                            existing.getId().toString(),
                            Map.of("scope", "single")
                    ));
                },
                () -> {
                    if (sessionToken != null && !sessionToken.isBlank()) {
                        sessionRepository.deleteByToken(sessionToken);
                    }
                    if (refreshToken != null && !refreshToken.isBlank()) {
                        sessionRepository.deleteByRefreshToken(refreshToken);
                    }
                    authSessionCache.evictSession(sessionToken, refreshToken, null);
                    auditEventService.record(AuditEventService.AuditEvent.success(
                            AuditActions.AUTH_LOGOUT,
                            "session",
                            null,
                            Map.of("scope", "single", "fallback", true)
                    ));
                }
        );
    }

    public AuthSessionData refreshSessionByRefreshToken(String refreshToken, String ipAddress, String userAgent) {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw AppException.unauthorized("Refresh token is missing");
            }
            Optional<Session> cachedSession = authSessionCache.findByRefreshToken(refreshToken)
                    .flatMap(cachedRef -> sessionRepository.findByIdWithUserAndPermissions(cachedRef.sessionId()));

            Session existingSession = cachedSession.or(() -> sessionRepository.findByRefreshTokenAndRefreshExpiresAtAfter(refreshToken, OffsetDateTime.now()))
                    .orElseThrow(() -> AppException.unauthorized("Refresh token is invalid or expired"));

            if (!refreshToken.equals(existingSession.getRefreshToken()) || existingSession.getRefreshExpiresAt().isBefore(OffsetDateTime.now())) {
                authSessionCache.evictSession(existingSession.getToken(), existingSession.getRefreshToken(), existingSession.getUser().getId());
                throw AppException.unauthorized("Refresh token is invalid or expired");
            }

            User user = existingSession.getUser();
            sessionRepository.delete(existingSession);
            authSessionCache.evictSession(existingSession.getToken(), existingSession.getRefreshToken(), user.getId());
            Session newSession = createSession(user, ipAddress, userAgent);
            authSessionCache.cacheSession(newSession);
            auditEventService.record(AuditEventService.AuditEvent.success(
                    AuditActions.AUTH_REFRESH_SUCCESS,
                    "session",
                    newSession.getId().toString(),
                    Map.of("rotatedFromSessionId", existingSession.getId().toString())
            ));
            return toAuthSessionData(user, newSession);
        } catch (AppException ex) {
            auditEventService.record(AuditEventService.AuditEvent.failure(
                    AuditActions.AUTH_REFRESH_FAILURE,
                    "auth",
                    null,
                    ex.getCode(),
                    null,
                    ipAddress,
                    userAgent,
                    Map.of()
            ));
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ManagedSessionData> listSessions(UUID userId, String currentSessionToken) {
        return sessionRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(Session::getCreatedAt).reversed())
                .map(session -> new ManagedSessionData(
                        session.getId(),
                        session.getToken().equals(currentSessionToken),
                        session.getCreatedAt(),
                        session.getExpiresAt(),
                        session.getRefreshExpiresAt(),
                        session.getIpAddress(),
                        session.getUserAgent()
                ))
                .toList();
    }

    public boolean revokeSession(UUID userId, UUID sessionId, String currentSessionToken) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> AppException.notFound("Session not found"));
        boolean current = session.getToken().equals(currentSessionToken);
        sessionRepository.delete(session);
        authSessionCache.evictSession(session.getToken(), session.getRefreshToken(), userId);
        auditEventService.record(AuditEventService.AuditEvent.success(
                AuditActions.AUTH_SESSION_REVOKE,
                "session",
                sessionId.toString(),
                Map.of("current", current)
        ));
        return current;
    }

    public void logoutAll(UUID userId) {
        sessionRepository.deleteByUserId(userId);
        authSessionCache.evictAllUserSessions(userId);
        auditEventService.record(AuditEventService.AuditEvent.success(
                AuditActions.AUTH_LOGOUT_ALL,
                "session",
                userId.toString(),
                Map.of("scope", "all")
        ));
    }

    public void reauthenticate(User user, String password) {
        String email = normalizeEmail(user.getEmail());
        try {
            Account account = accountRepository.findByProviderIdAndAccountId(LOCAL_PROVIDER, email)
                    .orElseThrow(() -> AppException.unauthorized("Re-authentication failed"));
            if (account.getPasswordHash() == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
                throw AppException.unauthorized("Re-authentication failed");
            }
            auditEventService.record(AuditEventService.AuditEvent.success(
                    AuditActions.AUTH_REAUTH_SUCCESS,
                    "user",
                    user.getId().toString(),
                    Map.of("provider", LOCAL_PROVIDER)
            ));
        } catch (AppException ex) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("provider", LOCAL_PROVIDER);
            metadata.put("userId", user.getId().toString());
            auditEventService.record(AuditEventService.AuditEvent.failure(
                    AuditActions.AUTH_REAUTH_FAILURE,
                    "auth",
                    user.getId().toString(),
                    ex.getCode(),
                    email,
                    null,
                    null,
                    metadata
            ));
            throw ex;
        }
    }

    private Session createSession(User user, String ipAddress, String userAgent) {
        Session session = new Session();
        session.setUser(user);
        session.setToken(authTokenService.generateToken());
        session.setExpiresAt(OffsetDateTime.now().plus(authProperties.getSession().getTtl()));
        session.setRefreshToken(authTokenService.generateToken());
        session.setRefreshExpiresAt(OffsetDateTime.now().plus(authProperties.getRefresh().getTtl()));
        session.setIpAddress(trimToNull(ipAddress));
        session.setUserAgent(trimToNull(userAgent));
        return sessionRepository.save(session);
    }

    private AuthSessionData toAuthSessionData(User user, Session session) {
        return new AuthSessionData(
                user,
                session.getToken(),
                session.getExpiresAt(),
                session.getRefreshToken(),
                session.getRefreshExpiresAt()
        );
    }

    private Optional<Session> resolveSessionForLogout(String sessionToken, String refreshToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            Optional<Session> sessionOpt = sessionRepository.findByToken(sessionToken);
            if (sessionOpt.isPresent()) {
                return sessionOpt;
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            return sessionRepository.findByRefreshToken(refreshToken);
        }
        return Optional.empty();
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

    public record AuthSessionData(
            User user,
            String token,
            OffsetDateTime expiresAt,
            String refreshToken,
            OffsetDateTime refreshExpiresAt
    ) {
    }

    public record ManagedSessionData(
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
