package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.fasterxml.jackson.databind.JsonNode;

/** Save or update an unfinished create form. {@code payload} is the form state as a JSON object. */
public record RiskFormDraftRequest(RiskType riskType, Long riskProjectId, JsonNode payload) {
}
