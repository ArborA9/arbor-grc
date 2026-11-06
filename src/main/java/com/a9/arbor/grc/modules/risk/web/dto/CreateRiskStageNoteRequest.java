package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRiskStageNoteRequest(
        @NotBlank @Size(max = 8000) String body,
        /** Admin/Owner only: Contributor requests are forced false. */
        Boolean internalOnly
) {
}
