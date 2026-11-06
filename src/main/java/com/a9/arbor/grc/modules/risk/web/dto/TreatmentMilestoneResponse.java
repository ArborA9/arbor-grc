package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.TreatmentMilestone;

import java.time.Instant;
import java.time.LocalDate;

public record TreatmentMilestoneResponse(
        Long id,
        Long treatmentPlanId,
        String milestoneTitle,
        LocalDate dueDate,
        boolean completed,
        LocalDate completedDate,
        Long completedByUserId,
        String progressNotes,
        Instant createdAt,
        Instant updatedAt
) {
    public static TreatmentMilestoneResponse from(TreatmentMilestone m) {
        return new TreatmentMilestoneResponse(
                m.getId(),
                m.getTreatmentPlanId(),
                m.getMilestoneTitle(),
                m.getDueDate(),
                m.isCompleted(),
                m.getCompletedDate(),
                m.getCompletedByUserId(),
                m.getProgressNotes(),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
