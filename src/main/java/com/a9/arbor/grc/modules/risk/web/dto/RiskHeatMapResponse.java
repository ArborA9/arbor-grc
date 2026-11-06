package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.HeatMapScoreBasis;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskType;

import java.util.List;

public record RiskHeatMapResponse(
        RiskType riskType,
        HeatMapScoreBasis scoreBasis,
        boolean openOnly,
        int gridSize,
        String rowAxis,
        String colAxis,
        List<RiskLevel> rows,
        List<RiskLevel> cols,
        List<RiskHeatMapCellResponse> cells,
        long total,
        long closedExcluded
) {
}
