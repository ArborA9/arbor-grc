package com.a9.arbor.grc.modules.capa.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;

public record CapaAvailableFindingResponse(
        Long id,
        String displayCode,
        String title,
        String severity,
        String controlRef,
        Long auditId,
        String auditName
) {
    public static CapaAvailableFindingResponse from(GrcAuditFinding f, GrcAudit audit) {
        return new CapaAvailableFindingResponse(
                f.getId(),
                f.getDisplayCode(),
                f.getTitle(),
                f.getSeverity() != null ? f.getSeverity().name() : null,
                f.getControlRef(),
                f.getAuditId(),
                audit != null ? audit.getName() : null);
    }
}
