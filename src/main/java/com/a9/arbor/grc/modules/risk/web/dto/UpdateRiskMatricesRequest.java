package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRiskMatricesRequest(
        @NotNull RiskLevel[][] inherent,
        @NotNull RiskLevel[][] residual,
        int gridSize,
        @NotNull RiskLevel[] rowLevels,
        @NotNull RiskLevel[] colLevels,
        @NotNull @Size(max = 1000) String changeReason
) {
}
