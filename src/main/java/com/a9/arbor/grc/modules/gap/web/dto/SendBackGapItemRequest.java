package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendBackGapItemRequest(
        @NotBlank @Size(max = 8000) String reason,
        /** Shown to the Contributor on the returned task. Required for non-SAMA. */
        GapImplementationStatus implementationStatus
) {
}
