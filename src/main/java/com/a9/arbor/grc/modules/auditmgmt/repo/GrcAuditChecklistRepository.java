package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrcAuditChecklistRepository extends JpaRepository<GrcAuditChecklist, Long> {
    List<GrcAuditChecklist> findByAuditIdOrderBySortOrderAscIdAsc(Long auditId);

    Optional<GrcAuditChecklist> findByIdAndAuditId(Long id, Long auditId);

    boolean existsByAuditIdAndTemplateKey(Long auditId, ChecklistTemplateKey templateKey);

    boolean existsByAuditIdAndNameIgnoreCase(Long auditId, String name);

    int countByAuditId(Long auditId);

    void deleteByAuditId(Long auditId);
}
