package com.a9.arbor.grc.modules.risk.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * FCA Design × Performance control score (3×3) and consolidated roll-up.
 * Roll-up does not drive residual for ERM / Unified (manual L×I).
 */
public final class FcaControlEffectiveness {

    public enum Axis {
        SATISFACTORY,
        NEEDS_IMPROVEMENT,
        UNSATISFACTORY;

        public static Axis parse(String raw) {
            if (raw == null || raw.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Control Design/Performance is required");
            }
            String t = raw.trim().toUpperCase().replace('-', ' ').replace('_', ' ');
            t = t.replaceAll("\\s+", " ").trim();
            return switch (t) {
                case "SATISFACTORY" -> SATISFACTORY;
                case "NEEDS IMPROVEMENT" -> NEEDS_IMPROVEMENT;
                case "UNSATISFACTORY" -> UNSATISFACTORY;
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown control axis value: " + raw);
            };
        }

        public String label() {
            return switch (this) {
                case SATISFACTORY -> "Satisfactory";
                case NEEDS_IMPROVEMENT -> "Needs Improvement";
                case UNSATISFACTORY -> "Unsatisfactory";
            };
        }
    }

    /** Design ↓ / Performance → : Satisfactory, Needs Improvement, Unsatisfactory. */
    private static final int[][] SCORE = {
            {5, 4, 2},
            {3, 3, 2},
            {1, 1, 1},
    };

    private FcaControlEffectiveness() {
    }

    public static int score(Axis design, Axis performance) {
        if (design == null || performance == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Control Design and Performance are required");
        }
        return SCORE[design.ordinal()][performance.ordinal()];
    }

    public static String word(int score) {
        return switch (score) {
            case 5 -> "Very strong";
            case 4 -> "Strong";
            case 3 -> "Moderate";
            case 2 -> "Weak";
            case 1 -> "Ineffective";
            default -> "-";
        };
    }

    public static boolean gapRequired(Axis design, Axis performance) {
        return design != Axis.SATISFACTORY || performance != Axis.SATISFACTORY;
    }

    /**
     * Rounded average of per-control scores; capped at 3 when any control has
     * Unsatisfactory performance.
     */
    public static Integer consolidate(List<Integer> scores, boolean anyUnsatisfactoryPerformance) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        int rounded = (int) Math.round(avg);
        if (anyUnsatisfactoryPerformance) {
            return Math.min(rounded, 3);
        }
        return rounded;
    }

    /** Map consolidated 1–5 score onto legacy {@link RiskLevel} for register filters. */
    public static RiskLevel toLegacyLevel(int score) {
        return switch (Math.min(5, Math.max(1, score))) {
            case 1 -> RiskLevel.VERY_LOW;
            case 2 -> RiskLevel.LOW;
            case 3 -> RiskLevel.MEDIUM;
            case 4 -> RiskLevel.HIGH;
            default -> RiskLevel.VERY_HIGH;
        };
    }
}
