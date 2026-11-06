package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistLibrary;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistLibraryItem;

import java.time.Instant;
import java.util.List;

public record AuditChecklistLibraryResponse(
        Long id,
        String name,
        ChecklistTemplateKey templateKey,
        boolean systemSeeded,
        int lineCount,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        List<ItemView> items
) {
    public record ItemView(
            Long id,
            int sortOrder,
            String clauseRef,
            String section,
            String title,
            String testFocus,
            String soaApplicability,
            String evidenceLives
    ) {
        public static ItemView from(GrcAuditChecklistLibraryItem item) {
            return new ItemView(
                    item.getId(),
                    item.getSortOrder(),
                    item.getClauseRef(),
                    item.getSection(),
                    item.getTitle(),
                    item.getTestFocus(),
                    item.getSoaApplicability(),
                    item.getEvidenceLives());
        }
    }

    public static AuditChecklistLibraryResponse from(GrcAuditChecklistLibrary lib, List<ItemView> items) {
        return new AuditChecklistLibraryResponse(
                lib.getId(),
                lib.getName(),
                lib.getTemplateKey(),
                lib.isSystemSeeded(),
                items.size(),
                lib.getCreatedByUserId(),
                lib.getCreatedAt(),
                lib.getUpdatedAt(),
                items);
    }
}
