package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskScoreHistory;

import java.time.Instant;

public record RiskScoreHistoryResponse(
        Long id,
        Long riskId,
        RiskLevel probability,
        RiskLevel impact,
        RiskLevel controlEffectiveness,
        RiskLevel inherentRisk,
        RiskLevel residualRisk,
        String reason,
        Long changedByUserId,
        Instant recordedAt
) {
    public static RiskScoreHistoryResponse from(RiskScoreHistory h) {
        return new RiskScoreHistoryResponse(
                h.getId(),
                h.getRiskId(),
                h.getProbability(),
                h.getImpact(),
                h.getControlEffectiveness(),
                h.getInherentRisk(),
                h.getResidualRisk(),
                h.getReason(),
                h.getChangedByUserId(),
                h.getRecordedAt());
    }
}
