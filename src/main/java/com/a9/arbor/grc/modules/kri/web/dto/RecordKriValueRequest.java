package com.a9.arbor.grc.modules.kri.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordKriValueRequest(
        @NotNull BigDecimal value,
        @Size(max = 4000) String notes
) {
}
