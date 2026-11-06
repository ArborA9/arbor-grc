package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRiskProjectRequest(
        @NotNull RiskType projectType,
        @NotBlank @Size(max = 255) String name,
        @NotNull @Min(2000) @Max(2100) Integer year,
        Long frameworkId
) {
}
