package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import jakarta.validation.constraints.Size;

/** Close Treat path: optional residual re-score (§13.4 / PPT slide 8). */
public record CloseRiskRequest(
        RiskLevel probability,
        RiskLevel impact,
        RiskLevel controlEffectiveness,
        @Size(max = 4000) String closeNotes
) {
}
