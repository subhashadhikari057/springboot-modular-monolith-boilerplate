package com.starterpack.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private final Cookie cookie = new Cookie();
    private final Cookie refreshCookie = new Cookie();
    private final Session session = new Session();
    private final Refresh refresh = new Refresh();
    private final Verification verification = new Verification();
    private final Cache cache = new Cache();
    private final Mail mail = new Mail();

    public AuthProperties() {
        refreshCookie.setName("rid");
    }

    public Cookie getCookie() {
        return cookie;
    }

    public Cookie getRefreshCookie() {
        return refreshCookie;
    }

    public Session getSession() {
        return session;
    }

    public Refresh getRefresh() {
        return refresh;
    }

    public Verification getVerification() {
        return verification;
    }

    public Cache getCache() {
        return cache;
    }

    public Mail getMail() {
        return mail;
    }

    public static class Cookie {
        private String name = "sid";
        private boolean secure = false;
        private String sameSite = "Lax";
        private String path = "/";
        private String domain;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }
    }

    public static class Session {
        private Duration ttl = Duration.ofDays(7);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static class Verification {
        private Duration ttl = Duration.ofMinutes(15);
        private boolean exposeTokenInResponse = true;
        private Duration resendCooldown = Duration.ofMinutes(1);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public boolean isExposeTokenInResponse() {
            return exposeTokenInResponse;
        }

        public void setExposeTokenInResponse(boolean exposeTokenInResponse) {
            this.exposeTokenInResponse = exposeTokenInResponse;
        }

        public Duration getResendCooldown() {
            return resendCooldown;
        }

        public void setResendCooldown(Duration resendCooldown) {
            this.resendCooldown = resendCooldown;
        }
    }

    public static class Refresh {
        private Duration ttl = Duration.ofDays(7);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static class Cache {
        private String prefix = "auth";
        private String userSessionSetPrefix = "auth:user-sessions";

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getUserSessionSetPrefix() {
            return userSessionSetPrefix;
        }

        public void setUserSessionSetPrefix(String userSessionSetPrefix) {
            this.userSessionSetPrefix = userSessionSetPrefix;
        }
    }

    public static class Mail {
        private String from = "no-reply@starterpack.local";
        private String verificationLinkBaseUrl = "http://localhost:3000/verify";
        private String passwordResetLinkBaseUrl = "http://localhost:3000/reset-password";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getVerificationLinkBaseUrl() {
            return verificationLinkBaseUrl;
        }

        public void setVerificationLinkBaseUrl(String verificationLinkBaseUrl) {
            this.verificationLinkBaseUrl = verificationLinkBaseUrl;
        }

        public String getPasswordResetLinkBaseUrl() {
            return passwordResetLinkBaseUrl;
        }

        public void setPasswordResetLinkBaseUrl(String passwordResetLinkBaseUrl) {
            this.passwordResetLinkBaseUrl = passwordResetLinkBaseUrl;
        }
    }
}
