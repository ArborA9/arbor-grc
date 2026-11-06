package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class GapScoring {

    private GapScoring() {
    }

    public static Integer scoreOf(GapImplementationStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case FULLY_IMPLEMENTED -> 100;
            case PARTIALLY_IMPLEMENTED -> 50;
            case PLANNED -> 25;
            case NOT_IMPLEMENTED -> 0;
        };
    }

    public static Result compute(List<GapItem> items) {
        int applicable = 0;
        int scored = 0;
        int sum = 0;
        for (GapItem item : items) {
            if (item.getApplicability() != GapApplicability.APPLICABLE) {
                continue;
            }
            applicable++;
            GapImplementationStatus effective = item.effectiveImplementationStatus();
            Integer effectiveScore = scoreOf(effective);
            if (effective != null && effectiveScore != null) {
                scored++;
                sum += effectiveScore;
            }
        }
        BigDecimal overall = null;
        String rag = null;
        if (applicable > 0 && scored == applicable) {
            overall = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(applicable * 100L), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            rag = ragFor(overall);
        } else if (applicable > 0 && scored > 0) {
            overall = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(applicable * 100L), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            rag = ragFor(overall);
        }
        return new Result(applicable, scored, overall, rag);
    }

    static String ragFor(BigDecimal score) {
        if (score == null) {
            return null;
        }
        double v = score.doubleValue();
        if (v <= 25) {
            return "CRITICAL";
        }
        if (v <= 50) {
            return "LOW";
        }
        if (v <= 75) {
            return "MODERATE";
        }
        if (v <= 80) {
            return "GOOD";
        }
        return "STRONG";
    }

    public record Result(int applicableCount, int scoredCount, BigDecimal overallScore, String ragStatus) {
    }
}
