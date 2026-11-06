package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditChecklistLibrary;

public record ChecklistTemplateResponse(
        ChecklistTemplateKey key,
        String name,
        int lineCount
) {
    public static ChecklistTemplateResponse from(AuditChecklistLibrary.Template t) {
        return new ChecklistTemplateResponse(t.key(), t.name(), t.lines().size());
    }
}
