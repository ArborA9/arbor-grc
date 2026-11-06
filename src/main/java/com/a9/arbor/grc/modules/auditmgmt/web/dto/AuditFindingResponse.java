package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;

import java.time.Instant;

public record AuditFindingResponse(
        Long id,
        Long checklistItemId,
        String displayCode,
        FindingSeverity severity,
        String title,
        String description,
        String recommendedAction,
        String controlRef,
        Long capaId,
        Instant createdAt
) {
    public static AuditFindingResponse from(GrcAuditFinding f) {
        return new AuditFindingResponse(
                f.getId(), f.getChecklistItemId(), f.getDisplayCode(), f.getSeverity(),
                f.getTitle(), f.getDescription(), f.getRecommendedAction(), f.getControlRef(),
                f.getCapaId(), f.getCreatedAt());
    }
}
