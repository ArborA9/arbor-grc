package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.FindingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFindingRequest(
        @NotNull FindingStatus status
) {
}
