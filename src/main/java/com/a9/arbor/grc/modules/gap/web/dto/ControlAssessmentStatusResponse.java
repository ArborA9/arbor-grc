package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;

/**
 * Latest Gap assessment status for a framework control: Doc 02 R-09 (risk form pull).
 */
public record ControlAssessmentStatusResponse(
        boolean found,
        Long gapAssessmentId,
        String gapAssessmentName,
        GapAssessmentStatus assessmentStatus,
        Long gapItemId,
        String nodeCode,
        GapApplicability applicability,
        GapItemReviewStatus reviewStatus,
        GapImplementationStatus implementationStatus,
        GapImplementationStatus finalImplementationStatus,
        GapImplementationStatus effectiveStatus
) {
    public static ControlAssessmentStatusResponse empty() {
        return new ControlAssessmentStatusResponse(
                false, null, null, null, null, null, null, null, null, null, null);
    }

    public static ControlAssessmentStatusResponse from(GapAssessment assessment, GapItem item) {
        return new ControlAssessmentStatusResponse(
                true,
                assessment.getId(),
                assessment.getName(),
                assessment.getStatus(),
                item.getId(),
                item.getNodeCode(),
                item.getApplicability(),
                item.getReviewStatus(),
                item.getImplementationStatus(),
                item.getFinalImplementationStatus(),
                item.effectiveImplementationStatus());
    }
}
