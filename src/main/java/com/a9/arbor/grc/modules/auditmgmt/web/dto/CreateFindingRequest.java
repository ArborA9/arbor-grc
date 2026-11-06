package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFindingRequest(
        Long checklistItemId,
        @NotNull FindingSeverity severity,
        @NotBlank String title,
        @NotBlank String description,
        String recommendedAction,
        String controlRef
) {
}
