package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.DpiaIntakeMode;
import com.a9.arbor.grc.modules.privacy.domain.DpiaResidualRisk;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDpiaRequest(
        @NotBlank @Size(max = 255) String title,
        Long ropaId,
        DpiaIntakeMode intakeMode,
        Boolean preScreeningRequired,
        @Size(max = 64) String preScreeningOutcome,
        @Size(max = 8000) String preScreeningNotes,
        @Size(max = 2000) String screeningQ1,
        @Size(max = 2000) String screeningQ2,
        Boolean screeningQ3,
        Boolean screeningQ4,
        Boolean screeningQ5,
        Boolean screeningQ6,
        Boolean screeningQ7,
        String questionsJson,
        String assessmentJson,
        String overviewRisksJson,
        @Size(max = 8000) String processingDescription,
        @Size(max = 8000) String necessityAssessment,
        @Size(max = 8000) String risksIdentified,
        @Size(max = 8000) String mitigationMeasures,
        DpiaResidualRisk residualRisk
) {
}
