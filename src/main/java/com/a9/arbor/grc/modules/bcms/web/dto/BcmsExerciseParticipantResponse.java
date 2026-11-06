package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.BcmsExerciseParticipant;

public record BcmsExerciseParticipantResponse(
        Long id,
        Long userId,
        String participantName
) {
    public static BcmsExerciseParticipantResponse from(BcmsExerciseParticipant p) {
        return new BcmsExerciseParticipantResponse(p.getId(), p.getUserId(), p.getParticipantName());
    }
}
