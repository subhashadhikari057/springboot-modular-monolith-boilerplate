package com.starterpack.backend.modules.auth.application.port;

import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.auth.application.model.CachedAuthContext;
import com.starterpack.backend.modules.auth.application.model.CachedRefreshRef;
import com.starterpack.backend.modules.users.domain.Session;

public interface AuthSessionCachePort {
    Optional<CachedAuthContext> findBySessionToken(String sessionToken);

    Optional<CachedRefreshRef> findByRefreshToken(String refreshToken);

    void cacheSession(Session session);

    void evictSession(String sessionToken, String refreshToken, UUID userId);

    void evictAllUserSessions(UUID userId);
}
