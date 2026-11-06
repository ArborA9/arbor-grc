package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record UpdateAuditEngagementRequest(
        @NotBlank String name,
        LocalDate startDate,
        LocalDate endDate,
        String scopeNotes,
        List<Long> orgUnitIds,
        @NotNull Long leadAuditorUserId,
        List<Long> teamMemberUserIds,
        List<EngagementScopeModule> scopeModules,
        List<Long> scopeNodeIds,
        String auditorOrganisation,
        Long auditeeOrgUnitId
) {
}
