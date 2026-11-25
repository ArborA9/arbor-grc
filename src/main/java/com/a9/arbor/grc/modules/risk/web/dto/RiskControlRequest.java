package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiskControlRequest(
        /** Optional; UI may send only Design × Performance. */
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 32) String design,
        @NotBlank @Size(max = 32) String performance,
        @Size(max = 4000) String controlGap
) {
}
