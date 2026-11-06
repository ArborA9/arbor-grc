package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.DpiaIntakeMode;
import com.a9.arbor.grc.modules.privacy.domain.DpiaResidualRisk;
import com.a9.arbor.grc.modules.privacy.domain.DpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDpia;

import java.time.Instant;

public record DpiaResponse(
        Long id,
        String displayId,
        String title,
        Long ropaId,
        DpiaIntakeMode intakeMode,
        Long assignedContributorId,
        Long workTaskId,
        Instant assignedAt,
        boolean preScreeningRequired,
        String preScreeningOutcome,
        String preScreeningNotes,
        String screeningQ1,
        String screeningQ2,
        Boolean screeningQ3,
        Boolean screeningQ4,
        Boolean screeningQ5,
        Boolean screeningQ6,
        Boolean screeningQ7,
        boolean fullDpiaMandatory,
        String questionsJson,
        String assessmentJson,
        String overviewRisksJson,
        String processingDescription,
        String necessityAssessment,
        String risksIdentified,
        String mitigationMeasures,
        DpiaResidualRisk residualRisk,
        DpiaStatus status,
        String reviewNotes,
        Long reviewedByUserId,
        Instant reviewedAt,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static DpiaResponse from(PrivacyDpia e) {
        return new DpiaResponse(
                e.getId(),
                e.getDisplayId(),
                e.getTitle(),
                e.getRopaId(),
                e.getIntakeMode(),
                e.getAssignedContributorId(),
                e.getWorkTaskId(),
                e.getAssignedAt(),
                e.isPreScreeningRequired(),
                e.getPreScreeningOutcome(),
                e.getPreScreeningNotes(),
                e.getScreeningQ1(),
                e.getScreeningQ2(),
                e.getScreeningQ3(),
                e.getScreeningQ4(),
                e.getScreeningQ5(),
                e.getScreeningQ6(),
                e.getScreeningQ7(),
                e.isFullDpiaMandatory(),
                e.getQuestionsJson(),
                e.getAssessmentJson(),
                e.getOverviewRisksJson(),
                e.getProcessingDescription(),
                e.getNecessityAssessment(),
                e.getRisksIdentified(),
                e.getMitigationMeasures(),
                e.getResidualRisk(),
                e.getStatus(),
                e.getReviewNotes(),
                e.getReviewedByUserId(),
                e.getReviewedAt(),
                e.getCreatedByUserId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
