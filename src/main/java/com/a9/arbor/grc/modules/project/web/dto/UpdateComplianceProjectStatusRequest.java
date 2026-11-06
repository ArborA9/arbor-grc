package com.a9.arbor.grc.modules.project.web.dto;

import com.a9.arbor.grc.modules.project.domain.ComplianceProjectStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateComplianceProjectStatusRequest(
        @NotNull ComplianceProjectStatus status
) {
}
