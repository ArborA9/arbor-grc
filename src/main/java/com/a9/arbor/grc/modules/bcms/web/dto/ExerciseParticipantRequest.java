package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.Size;

public record ExerciseParticipantRequest(
        Long userId,
        @Size(max = 255) String participantName
) {
}
