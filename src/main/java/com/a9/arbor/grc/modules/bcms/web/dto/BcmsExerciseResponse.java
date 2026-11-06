package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.BcmsExercise;
import com.a9.arbor.grc.modules.bcms.domain.ExerciseStatus;
import com.a9.arbor.grc.modules.bcms.domain.ExerciseType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BcmsExerciseResponse(
        Long id,
        Long projectId,
        String name,
        ExerciseType exerciseType,
        String scope,
        Long facilitatorUserId,
        String objectives,
        String scenariosTested,
        String results,
        String gapsIdentified,
        String lessonsLearned,
        LocalDate scheduledDate,
        ExerciseStatus status,
        Instant completedAt,
        Long createdByUserId,
        List<Long> planIds,
        List<BcmsExerciseParticipantResponse> participants,
        Instant createdAt,
        Instant updatedAt
) {
    public static BcmsExerciseResponse from(BcmsExercise e, List<Long> planIds,
                                            List<BcmsExerciseParticipantResponse> participants) {
        return new BcmsExerciseResponse(
                e.getId(),
                e.getProjectId(),
                e.getName(),
                e.getExerciseType(),
                e.getScope(),
                e.getFacilitatorUserId(),
                e.getObjectives(),
                e.getScenariosTested(),
                e.getResults(),
                e.getGapsIdentified(),
                e.getLessonsLearned(),
                e.getScheduledDate(),
                e.getStatus(),
                e.getCompletedAt(),
                e.getCreatedByUserId(),
                planIds,
                participants,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
