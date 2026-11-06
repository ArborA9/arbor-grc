package com.a9.arbor.grc.modules.soa.web.dto;

import com.a9.arbor.grc.modules.soa.domain.SoaApplicability;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSoaItemRequest(
        @NotNull SoaApplicability applicability,
        /** Required when applicability is NOT_APPLICABLE; optional when APPLICABLE (G-03). */
        @Size(max = 8000) String justification,
        @Size(max = 4000) String remarks
) {
}
