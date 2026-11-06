package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditMode;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditStandard;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateAuditRequest(
        @NotBlank String name,
        @NotNull AuditType auditType,
        AuditMode auditMode,
        AuditStandard standard,
        ChecklistTemplateKey checklistTemplate,
        Long frameworkId,
        Long leadAuditorUserId,
        String externalLeadEmail,
        String externalLeadName,
        LocalDate startDate,
        LocalDate endDate,
        String scopeNotes,
        List<Long> orgUnitIds,
        Long gapAssessmentId,
        List<Long> teamMemberUserIds,
        List<EngagementScopeModule> scopeModules,
        List<Long> scopeNodeIds,
        String auditorOrganisation,
        Long auditeeOrgUnitId,
        Long checklistLibraryId,
        List<Long> checklistLibraryIds
) {
}
