package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateGapItemRequest(
        GapApplicability applicability,
        @Size(max = 4000) String overrideJustification,
        GapImplementationStatus implementationStatus,
        @Min(0) @Max(5) Integer maturity,
        /** SAMA: five Yes/No/null gate answers; when present, maturity is derived. */
        List<Boolean> maturityGates,
        @Size(max = 8000) String observations,
        @Size(max = 8000) String gapDescription,
        @Size(max = 8000) String whatItMeans,
        @Size(max = 8000) String evidenceGuidance
) {
}
