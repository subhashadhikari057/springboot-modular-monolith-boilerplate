package com.starterpack.backend.modules.auth.infrastructure;

import java.time.Duration;

import com.starterpack.backend.modules.auth.application.port.VerificationResendThrottlePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisVerificationResendThrottle implements VerificationResendThrottlePort {
    private static final Logger logger = LoggerFactory.getLogger(RedisVerificationResendThrottle.class);

    private final StringRedisTemplate redis;

    public RedisVerificationResendThrottle(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean acquire(String key, Duration cooldown) {
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", cooldown);
            return Boolean.TRUE.equals(acquired);
        } catch (RuntimeException ex) {
            logger.warn("Redis unavailable while checking resend cooldown key={}: {}", key, ex.getMessage());
            return true;
        }
    }
}
