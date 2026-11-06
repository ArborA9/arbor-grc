package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.AcceptanceDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcceptanceOwnerDecideRequest(
        @NotNull AcceptanceDecision decision,
        @Size(max = 4000) String comments,
        @NotNull Boolean declaration
) {
}
