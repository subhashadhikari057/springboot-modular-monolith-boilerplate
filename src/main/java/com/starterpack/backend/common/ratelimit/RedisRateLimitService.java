package com.starterpack.backend.common.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class RedisRateLimitService implements RateLimitService {
    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimitService.class);

    private final StringRedisTemplate redis;
    private final RateLimitProperties properties;

    public RedisRateLimitService(StringRedisTemplate redis, RateLimitProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public RateLimitDecision evaluate(String policyName, HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return RateLimitDecision.permit();
        }

        RateLimitProperties.Policy policy = properties.getPolicies().get(policyName);
        if (policy == null) {
            throw new IllegalArgumentException("Unknown rate-limit policy: " + policyName);
        }

        Duration window = policy.getWindow();
        Duration blockDuration = policy.getBlockDuration();
        long windowSeconds = Math.max(1, window.getSeconds());
        long now = Instant.now().getEpochSecond();
        long bucket = now / windowSeconds;

        String keySignature = resolveSignature(policy.getKeys(), request);
        String base = properties.getPrefix() + ":" + policyName + ":" + keySignature;
        String blockKey = base + ":block";
        String countKey = base + ":count:" + bucket;

        try {
            Long blockedTtl = redis.getExpire(blockKey);
            if (blockedTtl != null && blockedTtl > 0) {
                return RateLimitDecision.blocked(blockedTtl);
            }

            Long count = redis.opsForValue().increment(countKey);
            if (count != null && count == 1L) {
                redis.expire(countKey, window.plusSeconds(1));
            }

            if (count != null && count > policy.getMaxRequests()) {
                redis.opsForValue().set(blockKey, "1", blockDuration);
                return RateLimitDecision.blocked(Math.max(1, blockDuration.getSeconds()));
            }

            return RateLimitDecision.permit();
        } catch (RuntimeException ex) {
            logger.warn("Rate limit check failed for policy={} message={}", policyName, ex.getMessage());
            return RateLimitDecision.permit();
        }
    }

    private String resolveSignature(List<String> keyParts, HttpServletRequest request) {
        List<String> values = new ArrayList<>();
        for (String keyPart : keyParts) {
            values.add(switch (keyPart) {
                case "ip" -> clientIp(request);
                case "route" -> request.getRequestURI();
                case "method" -> request.getMethod();
                case "userId" -> resolveUserId();
                default -> "unknown";
            });
        }
        return String.join(":", values);
    }

    private String resolveUserId() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication() == null ? null : org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            UUID id = user.getId();
            return id == null ? "anonymous" : id.toString();
        }
        return "anonymous";
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
