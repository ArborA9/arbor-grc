package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskStageNote;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;

import java.time.Instant;

public record RiskStageNoteResponse(
        Long id,
        Long riskId,
        RiskStatus workflowStage,
        String body,
        boolean internalOnly,
        Long createdByUserId,
        Instant createdAt
) {
    public static RiskStageNoteResponse from(RiskStageNote n) {
        return new RiskStageNoteResponse(
                n.getId(),
                n.getRiskId(),
                n.getWorkflowStage(),
                n.getBody(),
                n.isInternalOnly(),
                n.getCreatedByUserId(),
                n.getCreatedAt());
    }
}
