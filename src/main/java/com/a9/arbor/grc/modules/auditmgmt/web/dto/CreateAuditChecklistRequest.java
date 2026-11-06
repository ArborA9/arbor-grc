package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;

public record CreateAuditChecklistRequest(
        ChecklistTemplateKey templateKey,
        String name
) {
}
