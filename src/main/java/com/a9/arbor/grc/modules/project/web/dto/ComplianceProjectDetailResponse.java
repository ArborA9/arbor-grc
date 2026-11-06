package com.a9.arbor.grc.modules.project.web.dto;

import com.a9.arbor.grc.modules.gap.web.dto.GapAssessmentSummaryResponse;
import com.a9.arbor.grc.modules.soa.web.dto.SoaSummaryResponse;

import java.util.List;

public record ComplianceProjectDetailResponse(
        ComplianceProjectSummaryResponse project,
        List<SoaSummaryResponse> soas,
        List<GapAssessmentSummaryResponse> gapAssessments,
        /** Populated when curated IS risks were seeded on create (J9); otherwise null. */
        Integer seededIsRiskCount
) {
    public ComplianceProjectDetailResponse(
            ComplianceProjectSummaryResponse project,
            List<SoaSummaryResponse> soas,
            List<GapAssessmentSummaryResponse> gapAssessments) {
        this(project, soas, gapAssessments, null);
    }
}
