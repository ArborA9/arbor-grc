package com.a9.arbor.grc.modules.risk.domain;

/**
 * FCA approved 5×5 Likelihood × Impact rating lookup (not product banding).
 * Rows: Almost Certain → Extremely Unlikely ({@link RiskLevel#VERY_HIGH} → {@link RiskLevel#VERY_LOW}).
 * Cols: Very Low → Very High.
 * Display labels Low / Moderate / High / Significant / Extreme map to VL / L / M / H / VH.
 */
public final class FcaRiskMatrix {

    /**
     * Literal FCA map. Nine cells disagree with product banding (build was one band low).
     */
    private static final RiskLevel[][] LOOKUP = {
            // Impact → VL, L, M, H, VH
            {RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH, RiskLevel.VERY_HIGH}, // Almost Certain
            {RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH},       // Likely
            {RiskLevel.LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH},             // Possible
            {RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.MEDIUM},         // Unlikely
            {RiskLevel.VERY_LOW, RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.LOW, RiskLevel.MEDIUM},       // Extremely Unlikely
    };

    /** Brief default appetite: Moderate (internal {@link RiskLevel#LOW}). */
    public static final RiskLevel DEFAULT_APPETITE = RiskLevel.LOW;

    /** Brief auto-close: residual rating Low (internal {@link RiskLevel#VERY_LOW}). */
    public static final RiskLevel AUTO_CLOSE_MAX = RiskLevel.VERY_LOW;

    private FcaRiskMatrix() {
    }

    public static RiskLevel lookup(RiskLevel probability, RiskLevel impact) {
        if (probability == null || impact == null) {
            throw new IllegalArgumentException("Likelihood and Impact are required for FCA rating");
        }
        return LOOKUP[RiskMatrix.rowOf(probability)][RiskMatrix.colOf(impact)];
    }

    /** Rating key matching {@link ExcelRiskScoring#rating} string form (VERY_LOW…VERY_HIGH). */
    public static String ratingKey(RiskLevel probability, RiskLevel impact) {
        return lookup(probability, impact).name();
    }

    public static String action(RiskLevel probability, RiskLevel impact) {
        return actionForRating(lookup(probability, impact));
    }

    public static String actionForRating(RiskLevel rating) {
        return switch (rating) {
            case VERY_HIGH -> "Immediate board escalation: cease activity if possible";
            case HIGH -> "Urgent executive action: treatment mandatory";
            case MEDIUM -> "Treatment required";
            case LOW -> "Within appetite: monitor with review date";
            case VERY_LOW -> "Auto-close: residual Low";
        };
    }

    public static boolean withinAppetite(RiskLevel residualRating, RiskLevel appetite) {
        RiskLevel a = appetite != null ? appetite : DEFAULT_APPETITE;
        return residualRating != null && residualRating.ordinal() <= a.ordinal();
    }

    public static boolean autoClosable(RiskLevel residualRating) {
        return residualRating != null && residualRating.ordinal() <= AUTO_CLOSE_MAX.ordinal();
    }

    public static EvaluationDecision evaluation(RiskLevel residualRating, RiskLevel appetite) {
        if (autoClosable(residualRating)) {
            return EvaluationDecision.AUTO_CLOSE;
        }
        if (withinAppetite(residualRating, appetite)) {
            return EvaluationDecision.MONITOR;
        }
        return EvaluationDecision.TREATMENT_REQUIRED;
    }

    public static RiskLevel[][] levelGrid() {
        RiskLevel[][] out = new RiskLevel[5][5];
        for (int r = 0; r < 5; r++) {
            System.arraycopy(LOOKUP[r], 0, out[r], 0, 5);
        }
        return out;
    }

    /** Project FCA 5×5 onto active axes (3×3 / 4×4 tenants keep product banding instead). */
    public static boolean appliesTo(MatrixGridSize gridSize) {
        return gridSize == MatrixGridSize.FIVE;
    }
}
