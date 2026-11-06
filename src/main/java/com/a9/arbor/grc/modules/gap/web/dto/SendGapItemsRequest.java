package com.a9.arbor.grc.modules.gap.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SendGapItemsRequest(
        @NotEmpty List<Long> itemIds
) {
}
