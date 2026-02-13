package com.starterpack.backend.modules.audit.application;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.starterpack.backend.common.web.PageMeta;
import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.modules.audit.api.dto.AuditLogResponse;
import com.starterpack.backend.modules.audit.domain.AuditLog;
import com.starterpack.backend.modules.audit.domain.AuditResult;
import com.starterpack.backend.modules.audit.infrastructure.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PagedResponse<AuditLogResponse> listAdmin(
            int page,
            int size,
            UUID actorUserId,
            String action,
            String resourceType,
            String resourceId,
            AuditResult result,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditLog> rows = auditLogRepository.findAll(
                buildFilter(actorUserId, action, resourceType, resourceId, result, from, to),
                pageable
        );
        return new PagedResponse<>(rows.getContent().stream().map(AuditLogResponse::from).toList(), PageMeta.from(rows));
    }

    public PagedResponse<AuditLogResponse> listForActor(
            UUID actorUserId,
            int page,
            int size,
            String action,
            AuditResult result,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditLog> rows = auditLogRepository.findAll(
                buildFilter(actorUserId, action, null, null, result, from, to),
                pageable
        );
        return new PagedResponse<>(rows.getContent().stream().map(AuditLogResponse::from).toList(), PageMeta.from(rows));
    }

    private Specification<AuditLog> buildFilter(
            UUID actorUserId,
            String action,
            String resourceType,
            String resourceId,
            AuditResult result,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUser").get("id"), actorUserId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("action")), action.trim().toLowerCase()));
            }
            if (resourceType != null && !resourceType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("resourceType")), resourceType.trim().toLowerCase()));
            }
            if (resourceId != null && !resourceId.isBlank()) {
                predicates.add(cb.equal(root.get("resourceId"), resourceId.trim()));
            }
            if (result != null) {
                predicates.add(cb.equal(root.get("result"), result));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
