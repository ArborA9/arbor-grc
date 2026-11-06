package com.a9.arbor.grc.modules.kri.web.dto;

import com.a9.arbor.grc.modules.kri.domain.KriDirection;
import com.a9.arbor.grc.modules.kri.domain.KriFrequency;
import com.a9.arbor.grc.modules.kri.domain.KriStatus;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateKriRequest(
        @Size(max = 512) String name,
        Long linkedRiskId,
        @Size(max = 8000) String metricDescription,
        @Size(max = 64) String unit,
        KriFrequency frequency,
        Long ownerUserId,
        KriDirection direction,
        BigDecimal greenThreshold,
        BigDecimal amberThreshold,
        BigDecimal redThreshold,
        LocalDate nextDueDate,
        KriStatus status
) {
}
