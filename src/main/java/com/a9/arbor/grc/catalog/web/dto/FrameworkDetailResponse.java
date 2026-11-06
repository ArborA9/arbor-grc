package com.a9.arbor.grc.catalog.web.dto;

import java.util.List;

public record FrameworkDetailResponse(
        FrameworkSummaryResponse framework,
        List<FrameworkNodeResponse> nodes
) {
}
