package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditGranteeType;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateAccessGrantRequest(
        @NotNull Long userId,
        @NotNull AuditGranteeType granteeType,
        String organisation,
        String ndaReference,
        List<EngagementScopeModule> modules,
        Boolean canPreviewEvidence,
        Boolean canExportNotes,
        Boolean canDownloadEvidence,
        LocalDate validFrom,
        LocalDate validTo,
        Integer graceDays
) {
}
