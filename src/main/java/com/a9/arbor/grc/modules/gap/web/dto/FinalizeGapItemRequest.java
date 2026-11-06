package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Admin finalize. ISO/UAE IA use {@code finalImplementationStatus}; SAMA uses {@code finalMaturity}.
 * Service validates that the correct field is present for the assessment framework.
 */
public record FinalizeGapItemRequest(
        GapImplementationStatus finalImplementationStatus,
        @Min(0) @Max(5) Integer finalMaturity,
        @Size(max = 8000) String adminReviewNotes
) {
}
