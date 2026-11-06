package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record GapAssessmentSummaryResponse(
        Long id,
        Long projectId,
        Long frameworkId,
        String frameworkCode,
        String frameworkName,
        String frameworkVersion,
        Long soaId,
        String name,
        GapAssessmentType assessmentType,
        GapAssessmentStatus status,
        LocalDate targetDate,
        String scope,
        Long assessorUserId,
        BigDecimal overallScore,
        String ragStatus,
        int applicableCount,
        int scoredCount,
        int applicabilityOverrideCount,
        GapLayerScoreSummary layerScores,
        Integer assessmentDepth,
        Instant completedAt,
        Instant updatedAt
) {
    public static GapAssessmentSummaryResponse from(GapAssessment a) {
        return from(a, null, 0);
    }

    public static GapAssessmentSummaryResponse from(GapAssessment a, GapLayerScoreSummary layerScores) {
        return from(a, layerScores, 0);
    }

    public static GapAssessmentSummaryResponse from(GapAssessment a, GapLayerScoreSummary layerScores,
                                                    int applicabilityOverrideCount) {
        return new GapAssessmentSummaryResponse(
                a.getId(),
                a.getProjectId(),
                a.getFrameworkId(),
                a.getFrameworkCode(),
                a.getFrameworkName(),
                a.getFrameworkVersion(),
                a.getSoaId(),
                a.getName(),
                a.getAssessmentType(),
                a.getStatus(),
                a.getTargetDate(),
                a.getScope(),
                a.getAssessorUserId(),
                a.getOverallScore(),
                a.getRagStatus(),
                a.getApplicableCount(),
                a.getScoredCount(),
                applicabilityOverrideCount,
                layerScores,
                a.getAssessmentDepth() > 0 ? a.getAssessmentDepth() : 4,
                a.getCompletedAt(),
                a.getUpdatedAt());
    }
}
