package com.starterpack.backend.modules.auth.application;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.auth.api.dto.LoginRequest;
import com.starterpack.backend.modules.auth.api.dto.RegisterRequest;
import com.starterpack.backend.modules.auth.infrastructure.AuthSessionCache;
import com.starterpack.backend.modules.users.domain.Account;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.Session;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.infrastructure.AccountRepository;
import com.starterpack.backend.modules.users.infrastructure.RoleRepository;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import com.starterpack.backend.modules.users.infrastructure.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final AuthSessionCache authSessionCache;
    private final AuthTokenService authTokenService;

    public AuthAuthenticationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AccountRepository accountRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties,
            AuthSessionCache authSessionCache,
            AuthTokenService authTokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.authSessionCache = authSessionCache;
        this.authTokenService = authTokenService;
    }

    public AuthSessionData register(RegisterRequest request, String ipAddress, String userAgent) {
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
        authSessionCache.cacheSession(session);
        return toAuthSessionData(user, session);
    }

    public AuthSessionData login(LoginRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        Account account = accountRepository.findByProviderIdAndAccountId(LOCAL_PROVIDER, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (account.getPasswordHash() == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        Session session = createSession(user, ipAddress, userAgent);
        authSessionCache.cacheSession(session);
        return toAuthSessionData(user, session);
    }

    public void logout(String sessionToken, String refreshToken) {
        Optional<Session> session = resolveSessionForLogout(sessionToken, refreshToken);
        session.ifPresentOrElse(existing -> {
                    sessionRepository.delete(existing);
                    authSessionCache.evictSession(existing.getToken(), existing.getRefreshToken(), existing.getUser().getId());
                },
                () -> {
                    if (sessionToken != null && !sessionToken.isBlank()) {
                        sessionRepository.deleteByToken(sessionToken);
                    }
                    if (refreshToken != null && !refreshToken.isBlank()) {
                        sessionRepository.deleteByRefreshToken(refreshToken);
                    }
                    authSessionCache.evictSession(sessionToken, refreshToken, null);
                }
        );
    }

    public AuthSessionData refreshSessionByRefreshToken(String refreshToken, String ipAddress, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is missing");
        }
        Optional<Session> cachedSession = authSessionCache.findByRefreshToken(refreshToken)
                .flatMap(cachedRef -> sessionRepository.findByIdWithUserAndPermissions(cachedRef.sessionId()));

        Session existingSession = cachedSession.or(() -> sessionRepository.findByRefreshTokenAndRefreshExpiresAtAfter(refreshToken, OffsetDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));

        if (!refreshToken.equals(existingSession.getRefreshToken()) || existingSession.getRefreshExpiresAt().isBefore(OffsetDateTime.now())) {
            authSessionCache.evictSession(existingSession.getToken(), existingSession.getRefreshToken(), existingSession.getUser().getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
        }

        User user = existingSession.getUser();
        sessionRepository.delete(existingSession);
        authSessionCache.evictSession(existingSession.getToken(), existingSession.getRefreshToken(), user.getId());
        Session newSession = createSession(user, ipAddress, userAgent);
        authSessionCache.cacheSession(newSession);
        return toAuthSessionData(user, newSession);
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
}
