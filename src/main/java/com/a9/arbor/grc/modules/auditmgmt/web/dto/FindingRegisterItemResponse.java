package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;

import java.time.Instant;

/** Cross-audit open finding row for the findings register (F3.2). */
public record FindingRegisterItemResponse(
        long findingId,
        String displayCode,
        FindingSeverity severity,
        String title,
        String controlRef,
        long auditId,
        String auditName,
        AuditType auditType,
        boolean auditLocked,
        Long capaId,
        Instant createdAt
) {
    public static FindingRegisterItemResponse from(GrcAuditFinding f, GrcAudit audit) {
        return new FindingRegisterItemResponse(
                f.getId(),
                f.getDisplayCode(),
                f.getSeverity(),
                f.getTitle(),
                f.getControlRef(),
                audit.getId(),
                audit.getName(),
                audit.getAuditType(),
                audit.isLocked(),
                f.getCapaId(),
                f.getCreatedAt());
    }
}
