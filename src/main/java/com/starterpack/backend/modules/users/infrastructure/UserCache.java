package com.starterpack.backend.modules.users.infrastructure;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserCache {
    private static final String PREFIX = "users";
    private static final Logger cacheLogger = LoggerFactory.getLogger("CACHE");

    private final StringRedisTemplate redis;

    public UserCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String getById(String userId) {
        try {
            String key = keyById(userId);
            String value = redis.opsForValue().get(key);
            cacheLogger.info("CACHE_USER_GET key={} hit={}", key, value != null);
            return value;
        } catch (RuntimeException ex) {
            logCacheFailure("getById", ex);
            return null;
        }
    }

    public void putById(String userId, String json, Duration ttl) {
        try {
            String key = keyById(userId);
            redis.opsForValue().set(key, json, ttl);
            cacheLogger.info("CACHE_USER_PUT key={} ttl={}", key, ttl);
        } catch (RuntimeException ex) {
            logCacheFailure("putById", ex);
        }
    }

    public void invalidateById(String userId) {
        try {
            String key = keyById(userId);
            redis.delete(key);
            cacheLogger.info("CACHE_USER_EVICT key={}", key);
        } catch (RuntimeException ex) {
            logCacheFailure("invalidateById", ex);
        }
    }

    public void invalidateByEmail(String email) {
        try {
            String key = keyByEmail(email);
            redis.delete(key);
            cacheLogger.info("CACHE_USER_EVICT key={}", key);
        } catch (RuntimeException ex) {
            logCacheFailure("invalidateByEmail", ex);
        }
    }

    public void putByEmail(String email, String json, Duration ttl) {
        try {
            String key = keyByEmail(email);
            redis.opsForValue().set(key, json, ttl);
            cacheLogger.info("CACHE_USER_PUT key={} ttl={}", key, ttl);
        } catch (RuntimeException ex) {
            logCacheFailure("putByEmail", ex);
        }
    }

    public String getList(String listKey) {
        try {
            String value = redis.opsForValue().get(listKey);
            cacheLogger.info("CACHE_USER_LIST_GET key={} hit={}", listKey, value != null);
            return value;
        } catch (RuntimeException ex) {
            logCacheFailure("getList", ex);
            return null;
        }
    }

    public void putList(String listKey, String json, Duration ttl) {
        try {
            redis.opsForValue().set(listKey, json, ttl);
            cacheLogger.info("CACHE_USER_LIST_PUT key={} ttl={}", listKey, ttl);
        } catch (RuntimeException ex) {
            logCacheFailure("putList", ex);
        }
    }

    public void invalidateLists() {
        // For testing: clear all cached list pages.
        // Note: KEYS is not recommended for large datasets in production.
        try {
            java.util.Set<String> keys = redis.keys(PREFIX + ":list:*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
            cacheLogger.info("CACHE_USER_LIST_EVICT_ALL count={}", keys == null ? 0 : keys.size());
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
