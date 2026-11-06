package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.AcceptanceDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AcceptanceAdminDecideRequest(
        @NotNull AcceptanceDecision decision,
        @Size(max = 4000) String comments,
        @NotNull Boolean declaration,
        /** Admin Next Review Date after the three approvers. Falls back to the form date when omitted. */
        LocalDate nextReviewDate
) {
}
