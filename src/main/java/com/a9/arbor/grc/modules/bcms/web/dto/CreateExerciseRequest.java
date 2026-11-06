package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateExerciseRequest(
        Long projectId,
        @NotBlank @Size(max = 255) String name,
        @NotNull ExerciseType exerciseType,
        @Size(max = 4000) String scope,
        Long facilitatorUserId,
        @Size(max = 4000) String objectives,
        LocalDate scheduledDate,
        List<Long> planIds,
        List<ExerciseParticipantRequest> participants
) {
}
