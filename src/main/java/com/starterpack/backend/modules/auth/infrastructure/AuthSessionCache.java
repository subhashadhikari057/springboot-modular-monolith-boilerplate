package com.starterpack.backend.modules.auth.infrastructure;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.starterpack.backend.config.AuthProperties;
import com.starterpack.backend.modules.auth.application.model.CachedAuthContext;
import com.starterpack.backend.modules.users.domain.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionCache {
    private static final Logger logger = LoggerFactory.getLogger(AuthSessionCache.class);
    private static final Logger cacheLogger = LoggerFactory.getLogger("CACHE");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public AuthSessionCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            AuthProperties authProperties
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.authProperties = authProperties;
    }

    public Optional<CachedAuthContext> findBySessionToken(String sessionToken) {
        try {
            String json = redis.opsForValue().get(sessionKey(sessionToken));
            if (json == null) {
                cacheLogger.info("CACHE_AUTH_MISS sid={}", tokenId(sessionToken));
                return Optional.empty();
            }
            Optional<CachedAuthContext> context = deserializeContext(json);
            if (context.isEmpty()) {
                redis.delete(sessionKey(sessionToken));
                cacheLogger.info("CACHE_AUTH_STALE sid={} action=deleted", tokenId(sessionToken));
            } else {
                cacheLogger.info("CACHE_AUTH_HIT sid={}", tokenId(sessionToken));
            }
            return context;
        } catch (RuntimeException ex) {
            logger.warn("Redis unavailable while reading session cache: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<CachedRefreshRef> findByRefreshToken(String refreshToken) {
        try {
            String json = redis.opsForValue().get(refreshKey(refreshToken));
            if (json == null) {
                cacheLogger.info("CACHE_REFRESH_MISS rid={}", tokenId(refreshToken));
                return Optional.empty();
            }
            Optional<CachedRefreshRef> ref = deserializeRefreshRef(json);
            if (ref.isEmpty()) {
                redis.delete(refreshKey(refreshToken));
                cacheLogger.info("CACHE_REFRESH_STALE rid={} action=deleted", tokenId(refreshToken));
            } else {
                cacheLogger.info("CACHE_REFRESH_HIT rid={}", tokenId(refreshToken));
            }
            return ref;
        } catch (RuntimeException ex) {
            logger.warn("Redis unavailable while reading refresh cache: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void cacheSession(Session session) {
        cacheSession(CachedAuthContext.fromSession(session));
    }

    public void cacheSession(CachedAuthContext context) {
        Duration sessionTtl = ttlUntil(context.expiresAt());
        Duration refreshTtl = ttlUntil(context.refreshExpiresAt());
        if (sessionTtl.isNegative() || sessionTtl.isZero()) {
            return;
        }
        if (refreshTtl.isNegative() || refreshTtl.isZero()) {
            return;
        }

        try {
            String contextJson = objectMapper.writeValueAsString(context);
            redis.opsForValue().set(sessionKey(context.sessionToken()), contextJson, sessionTtl);

            CachedRefreshRef refreshRef = new CachedRefreshRef(
                    context.sessionId(),
                    context.userId(),
                    context.sessionToken(),
                    context.refreshToken(),
                    context.refreshExpiresAt()
            );
            String refreshJson = objectMapper.writeValueAsString(refreshRef);
            redis.opsForValue().set(refreshKey(context.refreshToken()), refreshJson, refreshTtl);

            redis.opsForSet().add(userSessionSetKey(context.userId()), context.sessionToken());
            redis.expire(userSessionSetKey(context.userId()), refreshTtl);
            cacheLogger.info(
                    "CACHE_AUTH_WRITE sid={} rid={} userId={} ttlSid={} ttlRid={}",
                    tokenId(context.sessionToken()),
                    tokenId(context.refreshToken()),
                    context.userId(),
                    sessionTtl,
                    refreshTtl
            );
        } catch (JsonProcessingException ex) {
            logger.warn("Failed to serialize auth cache payload: {}", ex.getMessage());
        } catch (RuntimeException ex) {
            logger.warn("Redis unavailable while writing session cache: {}", ex.getMessage());
        }
    }

    public void evictSession(String sessionToken, String refreshToken, UUID userId) {
        try {
            if (sessionToken != null && !sessionToken.isBlank()) {
                redis.delete(sessionKey(sessionToken));
            }
            if (refreshToken != null && !refreshToken.isBlank()) {
                redis.delete(refreshKey(refreshToken));
            }
            if (userId != null && sessionToken != null && !sessionToken.isBlank()) {
                redis.opsForSet().remove(userSessionSetKey(userId), sessionToken);
            }
            cacheLogger.info("CACHE_AUTH_EVICT sid={} rid={} userId={}", tokenId(sessionToken), tokenId(refreshToken), userId);
        } catch (RuntimeException ex) {
            logger.warn("Redis unavailable while evicting session cache: {}", ex.getMessage());
        }
    }

    public void evictAllUserSessions(UUID userId) {
        if (userId == null) {
            return;
        }
        String setKey = userSessionSetKey(userId);
        try {
            Set<String> sessionTokens = redis.opsForSet().members(setKey);
            if (sessionTokens != null) {
                for (String sessionToken : sessionTokens) {
                    Optional<CachedAuthContext> context = findBySessionToken(sessionToken);
                    context.ifPresent(cached -> redis.delete(refreshKey(cached.refreshToken())));
                    redis.delete(sessionKey(sessionToken));
                }
            }
            redis.delete(setKey);
            cacheLogger.info("CACHE_AUTH_EVICT_ALL userId={} sessions={}", userId, sessionTokens == null ? 0 : sessionTokens.size());
        } catch (RuntimeException ex) {
            logger.warn("Redis unavailable while evicting user sessions: {}", ex.getMessage());
        }
    }

    private Optional<CachedAuthContext> deserializeContext(String json) {
        try {
            CachedAuthContext context = objectMapper.readValue(json, CachedAuthContext.class);
            return Optional.of(context);
        } catch (JsonProcessingException ex) {
            logger.warn("Failed to deserialize cached auth context: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<CachedRefreshRef> deserializeRefreshRef(String json) {
        try {
            CachedRefreshRef ref = objectMapper.readValue(json, CachedRefreshRef.class);
            return Optional.of(ref);
        } catch (JsonProcessingException ex) {
            logger.warn("Failed to deserialize cached refresh reference: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Duration ttlUntil(OffsetDateTime expiresAt) {
        return Duration.between(OffsetDateTime.now(), expiresAt);
    }

    private String sessionKey(String sessionToken) {
        return authProperties.getCache().getPrefix() + ":sid:" + sessionToken;
    }

    private String refreshKey(String refreshToken) {
        return authProperties.getCache().getPrefix() + ":rid:" + refreshToken;
    }

    private String userSessionSetKey(UUID userId) {
        return authProperties.getCache().getUserSessionSetPrefix() + ":" + userId;
    }

    private String tokenId(String token) {
        if (token == null || token.isBlank()) {
            return "null";
        }
        return token.substring(0, Math.min(8, token.length()));
    }

    public record CachedRefreshRef(
            UUID sessionId,
            UUID userId,
            String sessionToken,
            String refreshToken,
            OffsetDateTime refreshExpiresAt
    ) {
    }
}
