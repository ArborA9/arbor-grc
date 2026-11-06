package com.a9.arbor.grc.modules.asset.web.dto;

import java.util.List;

/** Lists / valuation helpers for the Asset Register UI. */
public record AssetMetaResponse(
        List<String> classifications,
        List<TypeOption> assetTypes,
        List<String> assetSubTypes,
        List<String> hostingModels,
        List<String> dataResidencies,
        List<String> environments,
        List<String> lifecycleStatuses,
        List<String> triStates,
        List<ImpactScaleRow> impactScale,
        List<HandlingRow> handlingByConfidentiality
) {
    public record TypeOption(String value, String label) {
    }

    public record ImpactScaleRow(
            int level,
            String confidentiality,
            String integrity,
            String availability,
            String classification
    ) {
    }

    public record HandlingRow(
            int confidentiality,
            String sheetLabel,
            String labelling,
            String storage,
            String transfer,
            String access,
            String disposal
    ) {
    }
}
