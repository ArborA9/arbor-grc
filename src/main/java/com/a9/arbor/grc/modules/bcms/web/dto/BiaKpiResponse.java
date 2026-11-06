package com.a9.arbor.grc.modules.bcms.web.dto;

public record BiaKpiResponse(
        int inScope,
        int criticalCount,
        int normalCount,
        int tier1,
        int tier2,
        int tier3,
        int tier4,
        int tier5,
        int completeCount
) {
}
