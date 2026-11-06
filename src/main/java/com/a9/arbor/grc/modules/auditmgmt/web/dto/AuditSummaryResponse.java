package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditMode;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditStandard;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AuditSummaryResponse(
        Long id,
        String displayId,
        String name,
        AuditType auditType,
        AuditStandard auditStandard,
        AuditMode auditMode,
        Long frameworkId,
        String frameworkCode,
        String frameworkName,
        String frameworkVersion,
        GrcAuditStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String auditorOrganisation,
        Long auditeeOrgUnitId,
        Long leadAuditorUserId,
        boolean locked,
        Long gapAssessmentId,
        Instant auditorSignedAt,
        Instant adminSignedAt,
        Instant updatedAt,
        List<EngagementScopeModule> scopeModules,
        String leadAuditorName,
        int findingCount
) {
    public static AuditSummaryResponse from(GrcAudit a) {
        return from(a, List.of(), null, 0);
    }

    public static AuditSummaryResponse from(
            GrcAudit a,
            List<EngagementScopeModule> scopeModules,
            String leadAuditorName
    ) {
        return from(a, scopeModules, leadAuditorName, 0);
    }

    public static AuditSummaryResponse from(
            GrcAudit a,
            List<EngagementScopeModule> scopeModules,
            String leadAuditorName,
            int findingCount
    ) {
        List<EngagementScopeModule> modules = scopeModules == null ? List.of() : List.copyOf(scopeModules);
        return new AuditSummaryResponse(
                a.getId(), a.getDisplayId(), a.getName(), a.getAuditType(), a.getAuditStandard(), a.getAuditMode(),
                a.getFrameworkId(), a.getFrameworkCode(), a.getFrameworkName(), a.getFrameworkVersion(),
                a.getStatus(), a.getStartDate(), a.getEndDate(), a.getAuditorOrganisation(), a.getAuditeeOrgUnitId(),
                a.getLeadAuditorUserId(),
                a.isLocked(), a.getGapAssessmentId(), a.getAuditorSignedAt(), a.getAdminSignedAt(), a.getUpdatedAt(),
                modules,
                leadAuditorName,
                findingCount);
    }
}
