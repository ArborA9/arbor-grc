package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectValidationRequest(
        @NotBlank @Size(max = 4000) String reason
) {
}
