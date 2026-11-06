package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.ResidualScoringMode;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PostTreatmentRequest(
        RiskLevel controlEffectiveness,
        ResidualScoringMode residualScoringMode,
        Integer residualScore,
        RiskLevel targetControlEffectiveness,
        Integer targetResidualScore,
        Boolean targetResidualManual,
        LocalDate nextReviewDate,
        Boolean overrideAutoClose,
        @Size(max = 4000) String autoCloseOverrideJustification
) {
}
