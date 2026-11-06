package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;

import java.util.List;

public record AuditDetailResponse(
        AuditSummaryResponse audit,
        String scopeNotes,
        String executiveSummary,
        String returnReason,
        List<Long> orgUnitIds,
        List<AuditTeamMemberResponse> teamMembers,
        List<EngagementScopeModule> scopeModules,
        List<Long> scopeNodeIds,
        List<AuditChecklistResponse> checklists,
        List<AuditChecklistItemResponse> checklist,
        List<AuditFindingResponse> findings,
        Long gapAssessmentId,
        GapLayerScoreSummary gapLayerScores,
        List<AuditDomainNarrativeResponse> domainNarratives,
        List<AccessGrantResponse> accessGrants
) {
}
