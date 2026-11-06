package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrcAuditChecklistItemRepository extends JpaRepository<GrcAuditChecklistItem, Long> {
    List<GrcAuditChecklistItem> findByAuditIdOrderByNodeCodeAsc(Long auditId);

    List<GrcAuditChecklistItem> findByAuditIdOrderByChecklistIdAscSortOrderAscIdAsc(Long auditId);

    List<GrcAuditChecklistItem> findByChecklistIdOrderBySortOrderAscIdAsc(Long checklistId);

    Optional<GrcAuditChecklistItem> findByIdAndAuditId(Long id, Long auditId);

    long countByAuditId(Long auditId);

    int countByChecklistId(Long checklistId);

    void deleteByAuditId(Long auditId);

    void deleteByChecklistId(Long checklistId);
}
