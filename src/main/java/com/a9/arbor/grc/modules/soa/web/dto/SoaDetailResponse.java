package com.a9.arbor.grc.modules.soa.web.dto;

import java.util.List;

public record SoaDetailResponse(
        SoaSummaryResponse soa,
        List<SoaItemResponse> items
) {
}
