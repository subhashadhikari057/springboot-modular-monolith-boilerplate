package com.starterpack.backend.modules.auth.application;

import java.time.Duration;

import com.starterpack.backend.config.AuthProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {
    private final AuthProperties authProperties;

    public AuthCookieService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void writeSessionCookie(HttpHeaders headers, String token, Duration ttl) {
        writeCookie(headers, authProperties.getCookie(), token, ttl);
    }

    public void clearSessionCookie(HttpHeaders headers) {
        clearCookie(headers, authProperties.getCookie());
    }

    public void writeRefreshCookie(HttpHeaders headers, String token, Duration ttl) {
        writeCookie(headers, authProperties.getRefreshCookie(), token, ttl);
    }

    public void clearRefreshCookie(HttpHeaders headers) {
        clearCookie(headers, authProperties.getRefreshCookie());
    }

    private void writeCookie(HttpHeaders headers, AuthProperties.Cookie cookie, String token, Duration ttl) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookie.getName(), token)
                .httpOnly(true)
                .secure(cookie.isSecure())
                .path(cookie.getPath())
                .sameSite(cookie.getSameSite())
                .maxAge(ttl);

        if (cookie.getDomain() != null && !cookie.getDomain().isBlank()) {
            builder.domain(cookie.getDomain());
        }

        headers.add(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearCookie(HttpHeaders headers, AuthProperties.Cookie cookie) {
        writeCookie(headers, cookie, "", Duration.ZERO);
    }
}
