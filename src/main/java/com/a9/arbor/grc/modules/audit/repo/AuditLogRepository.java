package com.a9.arbor.grc.modules.audit.repo;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Insert + read only. Do not add update/delete helpers.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long countByCreatedAtAfter(Instant since);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:module IS NULL OR a.module = :module)
              AND (:actionType IS NULL OR a.actionType = :actionType)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    Page<AuditLog> findForExport(@Param("module") String module,
                                 @Param("actionType") AuditActionType actionType,
                                 Pageable pageable);
}
