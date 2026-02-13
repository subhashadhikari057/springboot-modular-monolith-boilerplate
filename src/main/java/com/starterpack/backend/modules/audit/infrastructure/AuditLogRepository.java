package com.starterpack.backend.modules.audit.infrastructure;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.starterpack.backend.modules.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    @Modifying
    @Query(value = """
            DELETE FROM audit_logs
            WHERE id IN (
                SELECT id
                FROM audit_logs
                WHERE occurred_at < :cutoff
                ORDER BY occurred_at
                LIMIT :batchSize
            )
            """, nativeQuery = true)
    int deleteBatchOlderThan(@Param("cutoff") OffsetDateTime cutoff, @Param("batchSize") int batchSize);
}
