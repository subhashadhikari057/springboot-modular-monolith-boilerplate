package com.starterpack.backend.modules.audit.application;

import java.time.OffsetDateTime;

import com.starterpack.backend.modules.audit.config.AuditRetentionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuditRetentionJob {
    private static final Logger logger = LoggerFactory.getLogger(AuditRetentionJob.class);

    private final AuditRetentionProperties properties;
    private final AuditRetentionExecutor auditRetentionExecutor;

    public AuditRetentionJob(AuditRetentionProperties properties, AuditRetentionExecutor auditRetentionExecutor) {
        this.properties = properties;
        this.auditRetentionExecutor = auditRetentionExecutor;
    }

    @Scheduled(cron = "${audit.retention.cron:0 30 2 * * *}", zone = "${audit.retention.zone:UTC}")
    public void runRetention() {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getDays() <= 0) {
            logger.warn("AUDIT_RETENTION_SKIPPED reason=invalid_days days={}", properties.getDays());
            return;
        }
        if (properties.getBatchSize() <= 0) {
            logger.warn("AUDIT_RETENTION_SKIPPED reason=invalid_batch_size batchSize={}", properties.getBatchSize());
            return;
        }

        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(properties.getDays());
        long totalDeleted = 0;
        long startedAt = System.currentTimeMillis();

        while (true) {
            int deleted = auditRetentionExecutor.deleteBatch(cutoff, properties.getBatchSize());
            totalDeleted += deleted;
            if (deleted < properties.getBatchSize()) {
                break;
            }
        }

        logger.info(
                "AUDIT_RETENTION_DONE cutoff={} days={} batchSize={} deleted={} durationMs={}",
                cutoff,
                properties.getDays(),
                properties.getBatchSize(),
                totalDeleted,
                System.currentTimeMillis() - startedAt
        );
    }
}
