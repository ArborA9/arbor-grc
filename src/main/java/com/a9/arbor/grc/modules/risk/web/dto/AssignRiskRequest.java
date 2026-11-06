package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssignRiskRequest(
        @NotNull Long userId,
        LocalDate dueDate,
        TreatmentOption treatmentOption,
        @Size(max = 8000) String treatmentPlan
) {
}
