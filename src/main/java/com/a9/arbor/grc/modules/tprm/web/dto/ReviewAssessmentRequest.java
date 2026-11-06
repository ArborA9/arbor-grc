package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.TprmVerdict;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReviewAssessmentRequest(
        @NotEmpty @Valid List<ReviewItem> items,
        boolean complete,
        /** When true, hand off Returned controls to the vendor (email + status RETURNED). Save alone does not. */
        boolean returnToVendor
) {
    public ReviewAssessmentRequest(List<ReviewItem> items, boolean complete) {
        this(items, complete, false);
    }

    public record ReviewItem(
            @NotNull Long itemId,
            @NotNull TprmVerdict verdict,
            String comment
    ) {
    }
}
