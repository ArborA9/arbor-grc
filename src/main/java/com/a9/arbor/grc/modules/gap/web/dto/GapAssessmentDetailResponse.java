package com.a9.arbor.grc.modules.gap.web.dto;

import java.util.List;

public record GapAssessmentDetailResponse(
        GapAssessmentSummaryResponse assessment,
        List<GapItemResponse> items
) {
}
