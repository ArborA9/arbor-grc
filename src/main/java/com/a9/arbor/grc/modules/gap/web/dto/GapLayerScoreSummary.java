package com.a9.arbor.grc.modules.gap.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Layer / domain scoring snapshot. Field overallUaeIa holds overall % for UAE IA, SAMA, or NCEMA
 * (JSON key kept for compatibility; UI labels by framework profile). */
public record GapLayerScoreSummary(
        LayerScore management,
        LayerScore technical,
        List<DomainScore> domains,
        LayerScore overallUaeIa
) {
    public record LayerScore(
            BigDecimal score,
            String rag,
            int applicableCount,
            int scoredCount
    ) {
    }

    public record DomainScore(
            String code,
            String name,
            BigDecimal score,
            String rag,
            int applicableCount,
            int scoredCount
    ) {
    }
}
