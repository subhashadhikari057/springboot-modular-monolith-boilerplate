package com.starterpack.backend.modules.audit.application;

import java.time.OffsetDateTime;

import com.starterpack.backend.modules.audit.infrastructure.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditRetentionExecutor {
    private final AuditLogRepository auditLogRepository;

    public AuditRetentionExecutor(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public int deleteBatch(OffsetDateTime cutoff, int batchSize) {
        return auditLogRepository.deleteBatchOlderThan(cutoff, batchSize);
    }
}
