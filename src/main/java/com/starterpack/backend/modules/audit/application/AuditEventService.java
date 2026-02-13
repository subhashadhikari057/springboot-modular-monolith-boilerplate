package com.starterpack.backend.modules.audit.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import com.starterpack.backend.modules.audit.domain.AuditLog;
import com.starterpack.backend.modules.audit.domain.AuditResult;
import com.starterpack.backend.modules.audit.infrastructure.AuditLogRepository;
import com.starterpack.backend.modules.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditEventService {
    private static final Logger logger = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    public AuditEventService(AuditLogRepository auditLogRepository, EntityManager entityManager) {
        this.auditLogRepository = auditLogRepository;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        try {
            HttpServletRequest request = currentRequest().orElse(null);

            UUID actorUserId = event.actorUserId() != null ? event.actorUserId() : currentActorUserId().orElse(null);
            String actorEmail = event.actorEmail() != null ? event.actorEmail() : currentActorEmail().orElse(null);

            AuditLog row = new AuditLog();
            if (actorUserId != null) {
                row.setActorUser(entityManager.getReference(User.class, actorUserId));
            }
            row.setActorEmail(actorEmail);
            row.setAction(event.action());
            row.setResourceType(event.resourceType());
            row.setResourceId(event.resourceId());
            row.setResult(event.result());
            row.setReasonCode(event.reasonCode());
            row.setIpAddress(firstNonBlank(event.ipAddress(), clientIp(request)));
            row.setUserAgent(firstNonBlank(event.userAgent(), request == null ? null : request.getHeader("User-Agent")));
            row.setRequestId(firstNonBlank(event.requestId(), request == null ? null : request.getHeader("X-Request-Id")));
            row.setMetadata(event.metadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(event.metadata()));
            auditLogRepository.save(row);
        } catch (RuntimeException ex) {
            logger.warn("AUDIT_WRITE_FAILED action={} message={}", event.action(), ex.getMessage());
        }
    }

    public record AuditEvent(
            String action,
            String resourceType,
            String resourceId,
            AuditResult result,
            String reasonCode,
            UUID actorUserId,
            String actorEmail,
            String ipAddress,
            String userAgent,
            String requestId,
            Map<String, Object> metadata
    ) {
        public static AuditEvent success(String action, String resourceType, String resourceId, Map<String, Object> metadata) {
            return new AuditEvent(action, resourceType, resourceId, AuditResult.SUCCESS, null, null, null, null, null, null, metadata);
        }

        public static AuditEvent failure(
                String action,
                String resourceType,
                String resourceId,
                String reasonCode,
                String actorEmail,
                String ipAddress,
                String userAgent,
                Map<String, Object> metadata
        ) {
            return new AuditEvent(
                    action,
                    resourceType,
                    resourceId,
                    AuditResult.FAILURE,
                    reasonCode,
                    null,
                    actorEmail,
                    ipAddress,
                    userAgent,
                    null,
                    metadata
            );
        }
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return Optional.of(attrs.getRequest());
        }
        return Optional.empty();
    }

    private Optional<UUID> currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user && user.getId() != null) {
            return Optional.of(user.getId());
        }
        return Optional.empty();
    }

    private Optional<String> currentActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.ofNullable(user.getEmail());
        }
        return Optional.empty();
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
