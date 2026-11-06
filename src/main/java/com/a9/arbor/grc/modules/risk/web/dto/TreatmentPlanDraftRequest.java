package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Structured treatment plan draft at risk create / update (§13.4 Treat path). */
public record TreatmentPlanDraftRequest(
        @Size(max = 8000) String actionDescription,
        Long actionOwnerId,
        /** Doc 02 R-10: custodian beside action owner. */
        Long custodianUserId,
        LocalDate startDate,
        LocalDate targetDate,
        @Size(max = 4000) String resourcesRequired,
        BigDecimal costEstimateAed,
        @Size(max = 4000) String evidenceRequired
) {
}
