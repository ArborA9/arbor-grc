package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.Size;

public record CancelRiskRequest(
        @Size(max = 2000) String reason
) {
}
