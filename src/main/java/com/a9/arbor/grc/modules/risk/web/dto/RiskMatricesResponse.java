package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;

import java.time.Instant;

public record RiskMatricesResponse(
        RiskLevel[][] inherent,
        RiskLevel[][] residual,
        int gridSize,
        RiskLevel[] rowLevels,
        RiskLevel[] colLevels,
        int version,
        String changeReason,
        Long updatedByUserId,
        Instant updatedAt,
        boolean usingDefaults,
        boolean gridLocked,
        String appetiteRating,
        Integer appetiteMaxResidual
) {
}
