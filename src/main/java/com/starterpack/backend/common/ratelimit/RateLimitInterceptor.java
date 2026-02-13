package com.starterpack.backend.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RateLimited rateLimited = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RateLimited.class);
        if (rateLimited == null) {
            rateLimited = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RateLimited.class);
        }
        if (rateLimited == null) {
            return true;
        }

        RateLimitDecision decision = rateLimitService.evaluate(rateLimited.value(), request);
        if (!decision.allowed()) {
            throw new RateLimitExceededException("Too many requests. Please retry later.", decision.retryAfterSeconds());
        }
        return true;
    }
}
