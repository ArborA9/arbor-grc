package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;

public record RiskHeatMapCellResponse(
        RiskLevel rowLevel,
        RiskLevel colLevel,
        int row,
        int col,
        long count,
        RiskLevel band
) {
}
