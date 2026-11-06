package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessGrant;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditGranteeType;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccessGrantResponse(
        Long id,
        String displayId,
        Long auditId,
        Long userId,
        AuditGranteeType granteeType,
        String organisation,
        String ndaReference,
        List<EngagementScopeModule> modules,
        String scopeBoundary,
        boolean canPreviewEvidence,
        boolean canExportNotes,
        boolean canDownloadEvidence,
        LocalDate validFrom,
        LocalDate validTo,
        int graceDays,
        LocalDate autoExpiry,
        boolean revoked,
        String accessStatus,
        String independenceCheck,
        long recordsViewed,
        Instant createdAt
) {
    public static AccessGrantResponse from(AuditAccessGrant g,
                                           List<EngagementScopeModule> modules,
                                           String accessStatus,
                                           String independenceCheck,
                                           long recordsViewed) {
        return new AccessGrantResponse(
                g.getId(), g.getDisplayId(), g.getAuditId(), g.getUserId(), g.getGranteeType(),
                g.getOrganisation(), g.getNdaReference(), modules, g.getScopeBoundary(),
                g.isCanPreviewEvidence(), g.isCanExportNotes(), g.isCanDownloadEvidence(),
                g.getValidFrom(), g.getValidTo(), g.getGraceDays(), g.autoExpiry(),
                g.isRevoked(), accessStatus, independenceCheck, recordsViewed, g.getCreatedAt());
    }
}
