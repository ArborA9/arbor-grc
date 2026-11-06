package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.Size;

public record ValidateApproveRequest(
        @Size(max = 4000) String validationNotes,
        @Size(max = 4000) String infosecStatusFinal
) {
}
