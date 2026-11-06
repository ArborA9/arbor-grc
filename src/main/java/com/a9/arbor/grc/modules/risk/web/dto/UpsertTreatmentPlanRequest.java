package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertTreatmentPlanRequest(
        @Size(max = 8000) String actionDescription,
        Long actionOwnerId,
        Long custodianUserId,
        LocalDate startDate,
        LocalDate targetDate,
        @Size(max = 4000) String resourcesRequired,
        BigDecimal costEstimateAed,
        @Size(max = 4000) String evidenceRequired,
        Boolean markInProgress
) {
}
