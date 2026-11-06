package com.a9.arbor.grc.modules.gap.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SendBackGapItemsRequest(
        @NotEmpty List<Long> itemIds,
        @NotBlank String reason
) {
}
