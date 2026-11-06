package com.a9.arbor.grc.modules.task.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** ISO uses implementationStatus; SAMA uses maturityGates (preferred) or maturity 0–5. */
public record SubmitGapTaskRequest(
        GapImplementationStatus implementationStatus,
        @Min(0) @Max(5) Integer maturity,
        List<Boolean> maturityGates,
        String observations,
        String gapDescription,
        @Size(max = 8000) String contributorComments
) {
}
