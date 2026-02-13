package com.starterpack.backend.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

public interface RateLimitService {
    RateLimitDecision evaluate(String policyName, HttpServletRequest request);
}
