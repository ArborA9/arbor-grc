package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoveryPlanProcedureRequest(
        Long biaId,
        @NotBlank @Size(max = 255) String processName,
        @Size(max = 8000) String procedureText,
        int sortOrder
) {
}
