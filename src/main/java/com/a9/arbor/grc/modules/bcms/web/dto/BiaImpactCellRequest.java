package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotBlank;

public record BiaImpactCellRequest(
        @NotBlank String category,
        @NotBlank String band,
        Integer rating,
        String remarks
) {
}
