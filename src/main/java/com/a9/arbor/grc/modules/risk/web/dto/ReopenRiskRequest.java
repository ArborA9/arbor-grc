package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReopenRiskRequest(
        @NotBlank @Size(max = 2000) String reason
) {
}
