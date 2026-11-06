package com.a9.arbor.grc.modules.kri.domain;

import java.math.BigDecimal;

/** RAG / trend helpers for KRI thresholds (§14.1). */
public final class KriScoring {

    private KriScoring() {
    }

    public static KriRag rag(KriDirection direction, BigDecimal value,
                            BigDecimal green, BigDecimal amber, BigDecimal red) {
        if (direction == KriDirection.HIGHER_IS_WORSE) {
            // green < amber < red
            if (value.compareTo(red) >= 0) {
                return KriRag.RED;
            }
            if (value.compareTo(amber) >= 0) {
                return KriRag.AMBER;
            }
            return KriRag.GREEN;
        }
        // LOWER_IS_WORSE: red < amber < green
        if (value.compareTo(red) <= 0) {
            return KriRag.RED;
        }
        if (value.compareTo(amber) <= 0) {
            return KriRag.AMBER;
        }
        return KriRag.GREEN;
    }

    public static KriTrend trend(KriDirection direction, BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null) {
            return KriTrend.STABLE;
        }
        int cmp = current.compareTo(previous);
        if (cmp == 0) {
            return KriTrend.STABLE;
        }
        boolean improved = direction == KriDirection.HIGHER_IS_WORSE ? cmp < 0 : cmp > 0;
        return improved ? KriTrend.IMPROVING : KriTrend.DETERIORATING;
    }

    public static void validateThresholds(KriDirection direction,
                                          BigDecimal green, BigDecimal amber, BigDecimal red) {
        if (direction == KriDirection.HIGHER_IS_WORSE) {
            if (!(green.compareTo(amber) < 0 && amber.compareTo(red) < 0)) {
                throw new IllegalArgumentException(
                        "For Higher-is-worse: green < amber < red thresholds required");
            }
        } else if (!(red.compareTo(amber) < 0 && amber.compareTo(green) < 0)) {
            throw new IllegalArgumentException(
                    "For Lower-is-worse: red < amber < green thresholds required");
        }
    }
}
