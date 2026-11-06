package com.a9.arbor.grc.modules.asset.web.dto;

/** Dashboard-lite KPIs for the asset list. */
public record AssetKpiResponse(
        long total,
        long active,
        long disposed,
        long tier1Or2,
        long secretOrSensitive
) {
}
