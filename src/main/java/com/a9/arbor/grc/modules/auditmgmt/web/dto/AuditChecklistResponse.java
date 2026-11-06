package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklist;

import java.util.List;

public record AuditChecklistResponse(
        Long id,
        ChecklistTemplateKey templateKey,
        String name,
        int sortOrder,
        List<AuditChecklistItemResponse> items
) {
    public static AuditChecklistResponse from(GrcAuditChecklist c, List<AuditChecklistItemResponse> items) {
        return new AuditChecklistResponse(c.getId(), c.getTemplateKey(), c.getName(), c.getSortOrder(), items);
    }
}
