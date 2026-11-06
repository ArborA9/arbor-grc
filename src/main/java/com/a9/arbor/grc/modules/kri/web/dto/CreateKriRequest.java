package com.a9.arbor.grc.modules.kri.web.dto;

import com.a9.arbor.grc.modules.kri.domain.KriDirection;
import com.a9.arbor.grc.modules.kri.domain.KriFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateKriRequest(
        @NotBlank @Size(max = 512) String name,
        Long linkedRiskId,
        @Size(max = 8000) String metricDescription,
        @Size(max = 64) String unit,
        @NotNull KriFrequency frequency,
        @NotNull Long ownerUserId,
        @NotNull KriDirection direction,
        @NotNull BigDecimal greenThreshold,
        @NotNull BigDecimal amberThreshold,
        @NotNull BigDecimal redThreshold,
        LocalDate nextDueDate
) {
}
