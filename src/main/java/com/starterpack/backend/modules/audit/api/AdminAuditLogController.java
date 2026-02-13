package com.starterpack.backend.modules.audit.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.starterpack.backend.common.error.AppException;
import com.starterpack.backend.common.web.PagedResponse;
import com.starterpack.backend.modules.audit.api.dto.AuditLogResponse;
import com.starterpack.backend.modules.audit.application.AuditLogService;
import com.starterpack.backend.modules.audit.domain.AuditResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Audit Logs", description = "Administrative audit trail access")
@Validated
public class AdminAuditLogController {
    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List audit logs", description = "Returns paginated audit logs with optional filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs returned", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid query params", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public PagedResponse<AuditLogResponse> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validatePagination(page, size);
        validateRange(from, to);
        return auditLogService.listAdmin(
                page - 1,
                size,
                actorUserId,
                action,
                resourceType,
                resourceId,
                result,
                from,
                to
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 1) {
            throw AppException.badRequest("page must be >= 1");
        }
        if (size < 1 || size > 100) {
            throw AppException.badRequest("size must be between 1 and 100");
        }
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw AppException.badRequest("from must be before or equal to to");
        }
    }
}
