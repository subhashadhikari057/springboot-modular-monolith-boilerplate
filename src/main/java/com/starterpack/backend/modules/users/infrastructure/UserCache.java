package com.starterpack.backend.modules.users.infrastructure;

import java.time.Duration;
import java.util.Optional;

import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.modules.users.api.dto.UserResponse;
import com.starterpack.backend.modules.users.application.port.UserListCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserCache implements UserListCachePort {
    private static final String PREFIX = "users";
    private static final Logger cacheLogger = LoggerFactory.getLogger("CACHE");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public UserCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
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

    @Override
    public Optional<PagedResponse<UserResponse>> getList(String listKey) {
        try {
            String json = redis.opsForValue().get(listKey);
            if (json == null) {
                cacheLogger.info("CACHE_USER_LIST_GET key={} hit=false", listKey);
                return Optional.empty();
            }
            PagedResponse<UserResponse> value = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructParametricType(PagedResponse.class, UserResponse.class)
            );
            cacheLogger.info("CACHE_USER_LIST_GET key={} hit=true", listKey);
            return Optional.of(value);
        } catch (JsonProcessingException ex) {
            redis.delete(listKey);
            cacheLogger.info("CACHE_USER_LIST_STALE key={} action=deleted", listKey);
            return Optional.empty();
        } catch (RuntimeException ex) {
            logCacheFailure("getList", ex);
            return Optional.empty();
        }
    }

    @Override
    public void putList(String listKey, PagedResponse<UserResponse> response, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redis.opsForValue().set(listKey, json, ttl);
            cacheLogger.info("CACHE_USER_LIST_PUT key={} ttl={}", listKey, ttl);
        } catch (JsonProcessingException ex) {
            cacheLogger.warn("CACHE_USER_LIST_SERIALIZE_FAILED key={} message={}", listKey, ex.getMessage());
        } catch (RuntimeException ex) {
            logCacheFailure("putList", ex);
        }
    }

    @Override
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
