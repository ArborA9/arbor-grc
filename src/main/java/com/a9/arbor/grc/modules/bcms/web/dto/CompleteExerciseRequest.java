package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.Size;

public record CompleteExerciseRequest(
        @Size(max = 4000) String scenariosTested,
        @Size(max = 8000) String results,
        @Size(max = 8000) String gapsIdentified,
        @Size(max = 4000) String lessonsLearned
) {
}
