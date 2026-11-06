package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Additional framework mapping on a risk (§22.2). */
public record RiskFrameworkMappingRequest(
        @NotNull Long frameworkId,
        @Size(max = 64) String frameworkCode,
        Long frameworkNodeId,
        @Size(max = 100) String controlRef,
        @Size(max = 512) String customRef
) {
}
