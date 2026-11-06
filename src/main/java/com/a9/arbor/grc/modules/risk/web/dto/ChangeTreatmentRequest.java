package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin-only: change treatment option after Contributor submission (§13.4). */
public record ChangeTreatmentRequest(
        @NotNull TreatmentOption treatmentOption,
        @NotNull @Size(max = 4000) String justification
) {
}
