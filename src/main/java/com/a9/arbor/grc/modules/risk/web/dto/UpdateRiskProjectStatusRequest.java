package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskProjectStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRiskProjectStatusRequest(
        @NotNull RiskProjectStatus status
) {
}
