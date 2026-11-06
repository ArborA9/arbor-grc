package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record RiskFormDraftResponse(
        Long id,
        RiskType riskType,
        Long riskProjectId,
        String title,
        JsonNode payload,
        int attachmentCount,
        Instant createdAt,
        Instant updatedAt
) {
}
