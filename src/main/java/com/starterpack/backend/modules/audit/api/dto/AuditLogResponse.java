package com.starterpack.backend.modules.audit.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.starterpack.backend.modules.audit.domain.AuditLog;
import com.starterpack.backend.modules.audit.domain.AuditResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Audit log record")
public record AuditLogResponse(
        UUID id,
        OffsetDateTime occurredAt,
        UUID actorUserId,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        AuditResult result,
        String reasonCode,
        String ipAddress,
        String userAgent,
        String requestId,
        Map<String, Object> metadata
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getOccurredAt(),
                auditLog.getActorUser() == null ? null : auditLog.getActorUser().getId(),
                auditLog.getActorEmail(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getResult(),
                auditLog.getReasonCode(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getRequestId(),
                auditLog.getMetadata()
        );
    }
}
