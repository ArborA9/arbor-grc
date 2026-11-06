package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapAssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateGapAssessmentRequest(
        @NotNull Long projectId,
        @NotNull Long frameworkId,
        @NotBlank @Size(max = 255) String name,
        @NotNull GapAssessmentType assessmentType,
        LocalDate targetDate,
        @Size(max = 8000) String scope,
        Long assessorUserId,
        /** UAE IA: 3 = L3 controls, 4 = L4 sub-controls. Ignored for other frameworks. Default 4. */
        Integer assessmentDepth
) {
}
