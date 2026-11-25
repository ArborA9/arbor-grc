package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.FcaControlEffectiveness;
import com.a9.arbor.grc.modules.risk.domain.RiskControl;

public record RiskControlResponse(
        Long id,
        int sortOrder,
        String description,
        String design,
        String performance,
        String controlGap,
        int score,
        String scoreLabel
) {
    public static RiskControlResponse from(RiskControl row) {
        FcaControlEffectiveness.Axis design = FcaControlEffectiveness.Axis.parse(row.getDesign());
        FcaControlEffectiveness.Axis perf = FcaControlEffectiveness.Axis.parse(row.getPerformance());
        int score = FcaControlEffectiveness.score(design, perf);
        return new RiskControlResponse(
                row.getId(),
                row.getSortOrder(),
                row.getDescription(),
                design.label(),
                perf.label(),
                row.getControlGap(),
                score,
                FcaControlEffectiveness.word(score));
    }
}
