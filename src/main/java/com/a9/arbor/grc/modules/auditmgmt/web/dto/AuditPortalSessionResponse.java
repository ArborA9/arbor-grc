package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;

import java.time.Instant;
import java.util.List;

public record AuditPortalSessionResponse(
        Long auditId,
        String auditName,
        String frameworkName,
        String frameworkCode,
        Instant accessExpiresAt,
        List<EngagementScopeModule> scopeModules,
        List<Long> scopeNodeIds,
        List<Long> orgUnitIds,
        AuditSummaryResponse audit
) {
}
