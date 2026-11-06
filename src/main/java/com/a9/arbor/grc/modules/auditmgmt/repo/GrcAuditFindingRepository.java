package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GrcAuditFindingRepository extends JpaRepository<GrcAuditFinding, Long> {
    List<GrcAuditFinding> findByAuditIdOrderByDisplayCodeAsc(Long auditId);

    List<GrcAuditFinding> findByCapaIdIsNullOrderByDisplayCodeAsc();
    Optional<GrcAuditFinding> findByIdAndAuditId(Long id, Long auditId);
    long countByAuditId(Long auditId);

    boolean existsByChecklistItemId(Long checklistItemId);

    @Query("""
            SELECT f.auditId, COUNT(f) FROM GrcAuditFinding f
            WHERE f.auditId IN :ids
            GROUP BY f.auditId
            """)
    List<Object[]> countGroupedByAuditId(Collection<Long> ids);

    @Query("""
            SELECT f FROM GrcAuditFinding f, GrcAudit a
            WHERE f.auditId = a.id AND a.locked = false
            ORDER BY f.createdAt DESC
            """)
    List<GrcAuditFinding> findOpenOnUnlockedAudits();

    @Query("""
            SELECT f.severity, COUNT(f) FROM GrcAuditFinding f, GrcAudit a
            WHERE f.auditId = a.id AND a.locked = false
            GROUP BY f.severity
            """)
    List<Object[]> countOpenFindingsBySeverity();
}
