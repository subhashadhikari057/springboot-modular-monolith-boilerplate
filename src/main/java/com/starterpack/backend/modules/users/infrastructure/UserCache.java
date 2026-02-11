package com.starterpack.backend.modules.users.infrastructure;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserCache {
    private static final String PREFIX = "users";
    private static final Logger logger = LoggerFactory.getLogger(UserCache.class);
    private static final Logger cacheLogger = LoggerFactory.getLogger("CACHE");

    private final StringRedisTemplate redis;

    public UserCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String getById(String userId) {
        try {
            return redis.opsForValue().get(keyById(userId));
        } catch (RuntimeException ex) {
            logCacheFailure("getById", ex);
            return null;
        }
    }

    public void putById(String userId, String json, Duration ttl) {
        try {
            redis.opsForValue().set(keyById(userId), json, ttl);
        } catch (RuntimeException ex) {
            logCacheFailure("putById", ex);
        }
    }

    public void invalidateById(String userId) {
        try {
            redis.delete(keyById(userId));
        } catch (RuntimeException ex) {
            logCacheFailure("invalidateById", ex);
        }
    }

    public void invalidateByEmail(String email) {
        try {
            redis.delete(keyByEmail(email));
        } catch (RuntimeException ex) {
            logCacheFailure("invalidateByEmail", ex);
        }
    }

    public void putByEmail(String email, String json, Duration ttl) {
        try {
            redis.opsForValue().set(keyByEmail(email), json, ttl);
        } catch (RuntimeException ex) {
            logCacheFailure("putByEmail", ex);
        }
    }

    public String getList(String listKey) {
        try {
            return redis.opsForValue().get(listKey);
        } catch (RuntimeException ex) {
            logCacheFailure("getList", ex);
            return null;
        }
    }

    public void putList(String listKey, String json, Duration ttl) {
        try {
            redis.opsForValue().set(listKey, json, ttl);
        } catch (RuntimeException ex) {
            logCacheFailure("putList", ex);
        }
    }

    public void invalidateLists() {
        // For testing: clear all cached list pages.
        // Note: KEYS is not recommended for large datasets in production.
        try {
            redis.delete(redis.keys(PREFIX + ":list:*"));
        } catch (RuntimeException ex) {
            logCacheFailure("invalidateLists", ex);
        }
    }

    private String keyById(String userId) {
        return PREFIX + ":by-id:" + userId;
    }

    private String keyByEmail(String email) {
        return PREFIX + ":by-email:" + email.toLowerCase();
    }

    private void logCacheFailure(String operation, RuntimeException ex) {
        cacheLogger.warn("CACHE_DISABLED {} (redis unavailable: {})", operation, ex.getMessage());
    }

    public void logHit(String key) {
        cacheLogger.debug("CACHE_HIT {}", key);
    }

    public void logMiss(String key) {
        cacheLogger.debug("CACHE_MISS {}", key);
    }
}
