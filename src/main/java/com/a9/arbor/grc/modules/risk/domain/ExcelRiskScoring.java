package com.a9.arbor.grc.modules.risk.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Likelihood × Impact product, CE% residual reduction, and score bands.
 *
 * <p>Bands (2026-09-24 pack): five names on every grid. Appetite max residual is
 * the top of the chosen appetite band on the active matrix. CE% rates stay
 * 5×5 / 4×4 / 3×3 as before.
 */
public final class ExcelRiskScoring {

    private ExcelRiskScoring() {
    }

    /** Rank on the active axis: left/bottom = 1, right/top = N (Excel sheet numbering). */
    public static int rowRank(MatrixAxes axes, RiskLevel probability) {
        int idx = axes.rowIndex(probability);
        if (idx < 0) {
            throw new IllegalArgumentException("Probability outside active axes: " + probability);
        }
        return axes.rowLevels().length - idx;
    }

    public static int colRank(MatrixAxes axes, RiskLevel impact) {
        int idx = axes.colIndex(impact);
        if (idx < 0) {
            throw new IllegalArgumentException("Impact outside active axes: " + impact);
        }
        return idx + 1;
    }

    /** Excel cell value = Probability rank × Impact rank. */
    public static int score(MatrixAxes axes, RiskLevel probability, RiskLevel impact) {
        return rowRank(axes, probability) * colRank(axes, impact);
    }

    /**
     * Control-effectiveness rank 1..N on the same scale as likelihood (low CE = 1).
     */
    public static int ceRank(MatrixAxes axes, RiskLevel controlEffectiveness) {
        int idx = axes.colIndex(controlEffectiveness);
        if (idx < 0) {
            idx = axes.rowIndex(controlEffectiveness);
            if (idx >= 0) {
                return axes.rowLevels().length - idx;
            }
            throw new IllegalArgumentException("Control effectiveness outside active axes: " + controlEffectiveness);
        }
        return idx + 1;
    }

    /**
     * Risk reduction % from CE rating. 5×5: 0 / 0.2 / 0.4 / 0.6 / 0.8.
     * 4×4: 0 / 0.25 / 0.5 / 0.75. 3×3: 0 / 0.4 / 0.8.
     */
    public static double reductionPercent(MatrixGridSize gridSize, int ceRank) {
        return switch (gridSize) {
            case THREE -> switch (Math.min(3, Math.max(1, ceRank))) {
                case 1 -> 0.0;
                case 2 -> 0.4;
                default -> 0.8;
            };
            case FOUR -> switch (Math.min(4, Math.max(1, ceRank))) {
                case 1 -> 0.0;
                case 2 -> 0.25;
                case 3 -> 0.5;
                default -> 0.75;
            };
            case FIVE -> switch (Math.min(5, Math.max(1, ceRank))) {
                case 1 -> 0.0;
                case 2 -> 0.2;
                case 3 -> 0.4;
                case 4 -> 0.6;
                default -> 0.8;
            };
        };
    }

    public static double reductionPercent(MatrixGridSize gridSize, MatrixAxes axes, RiskLevel controlEffectiveness) {
        return reductionPercent(gridSize, ceRank(axes, controlEffectiveness));
    }

    /** Residual = round(inherent × (1 − CE%)), floor 1. */
    public static int residualScore(int inherentScore, double reductionPercent) {
        int rounded = (int) Math.round(inherentScore * (1.0 - reductionPercent));
        return Math.max(1, rounded);
    }

    public static int residualScore(MatrixGridSize gridSize, MatrixAxes axes,
                                    int inherentScore, RiskLevel controlEffectiveness) {
        return residualScore(inherentScore, reductionPercent(gridSize, axes, controlEffectiveness));
    }

    /** Highest Likelihood × Impact product (3×3 → 9, 4×4 → 16, 5×5 → 25). */
    public static int maxProduct(MatrixGridSize gridSize) {
        return gridSize.size() * gridSize.size();
    }

    /**
     * Typed residual / treated-target scores. {@code null} is allowed (auto CE path).
     * Rejects 0, negatives, and values above N×N.
     */
    public static void requireProductScore(Integer score, MatrixGridSize gridSize, String field) {
        requireProductScore(score, gridSize, maxProduct(gridSize), field);
    }

    /**
     * Same as {@link #requireProductScore(Integer, MatrixGridSize, String)} with an extra cap
     * (Residual Risk Score cannot exceed Inherent Risk Score).
     */
    public static void requireProductScore(Integer score, MatrixGridSize gridSize, int maxInclusive, String field) {
        if (score == null) {
            return;
        }
        int max = Math.min(maxProduct(gridSize), Math.max(1, maxInclusive));
        if (score < 1 || score > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " must be a whole number from 1 to " + max);
        }
    }

    /**
     * Rating from the 2026-09-24 band pack (same five names on every grid).
     * 5×5: 1-4 VL, 5-8 Low, 9-12 Medium, 13-19 High, 20-25 VH.
     * 4×4: 1-2 VL, 3-4 Low, 6-8 Medium, 9-12 High, 16 VH (gaps fill to the lower band).
     * 3×3: 1 VL, 2 Low, 3-4 Medium, 6 High, 9 VH.
     */
    public static String rating(MatrixGridSize gridSize, int score) {
        return switch (gridSize) {
            case THREE -> {
                if (score >= 9) {
                    yield "VERY_HIGH";
                }
                if (score >= 6) {
                    yield "HIGH";
                }
                if (score >= 3) {
                    yield "MEDIUM";
                }
                if (score >= 2) {
                    yield "LOW";
                }
                yield "VERY_LOW";
            }
            case FOUR -> {
                if (score >= 16) {
                    yield "VERY_HIGH";
                }
                if (score >= 9) {
                    yield "HIGH";
                }
                if (score >= 6) {
                    yield "MEDIUM";
                }
                if (score >= 3) {
                    yield "LOW";
                }
                yield "VERY_LOW";
            }
            case FIVE -> {
                if (score >= 20) {
                    yield "VERY_HIGH";
                }
                if (score >= 13) {
                    yield "HIGH";
                }
                if (score >= 9) {
                    yield "MEDIUM";
                }
                if (score >= 5) {
                    yield "LOW";
                }
                yield "VERY_LOW";
            }
        };
    }

    public static String action(MatrixGridSize gridSize, int score) {
        return switch (gridSize) {
            case THREE -> {
                if (score >= 9) {
                    yield "Immediate action required: escalate to top management";
                }
                if (score >= 6) {
                    yield "Urgent treatment required: assign risk owner";
                }
                if (score >= 3) {
                    yield "Treatment plan required within 30 days";
                }
                if (score >= 2) {
                    yield "Monitor and review: treatment may be required";
                }
                yield "Accept and monitor: periodic review";
            }
            case FOUR -> {
                if (score >= 16) {
                    yield "Immediate escalation: board/executive level";
                }
                if (score >= 9) {
                    yield "Urgent treatment: senior management action";
                }
                if (score >= 6) {
                    yield "Treatment plan required: management action";
                }
                if (score >= 3) {
                    yield "Monitor and review: routine management";
                }
                yield "Accept: periodic monitoring only";
            }
            case FIVE -> {
                if (score >= 20) {
                    yield "Immediate board escalation: cease activity if possible";
                }
                if (score >= 13) {
                    yield "Urgent executive action: treatment mandatory";
                }
                if (score >= 9) {
                    yield "Treatment required";
                }
                if (score >= 5) {
                    yield "Within appetite: monitor with review date";
                }
                yield "Auto-close: residual Very Low";
            }
        };
    }

    /** Auto-close: Very Low band only (3×3 ≤ 1, 4×4 ≤ 2, 5×5 ≤ 4). */
    public static int autoCloseMaxScore(MatrixGridSize gridSize) {
        return switch (gridSize) {
            case THREE -> 1;
            case FOUR -> 2;
            case FIVE -> 4;
        };
    }

    public static boolean autoClosable(MatrixGridSize gridSize, int residualScore) {
        return residualScore <= autoCloseMaxScore(gridSize);
    }

    /**
     * Max residual still within the chosen appetite band on the active matrix.
     */
    public static int appetiteMaxResidual(MatrixGridSize gridSize, RiskLevel appetite) {
        RiskLevel a = appetite != null ? appetite : RiskLevel.LOW;
        return switch (gridSize) {
            case THREE -> switch (a) {
                case VERY_LOW -> 1;
                case LOW -> 2;
                case MEDIUM -> 4;
                case HIGH -> 6;
                case VERY_HIGH -> 9;
            };
            case FOUR -> switch (a) {
                case VERY_LOW -> 2;
                case LOW -> 4;
                case MEDIUM -> 8;
                case HIGH -> 12;
                case VERY_HIGH -> 16;
            };
            case FIVE -> switch (a) {
                case VERY_LOW -> 4;
                case LOW -> 8;
                case MEDIUM -> 12;
                case HIGH -> 19;
                case VERY_HIGH -> 25;
            };
        };
    }

    /**
     * Excel “Acceptance Required?” plus Business Rules: Terminate / Transfer never open the
     * Risk Acceptance sheet even if target residual is still above appetite.
     */
    public static boolean acceptanceSheetRequired(TreatmentOption treatment, Integer targetResidualScore,
                                                 MatrixGridSize gridSize, RiskLevel appetite) {
        if (treatment == TreatmentOption.TERMINATE || treatment == TreatmentOption.TRANSFER) {
            return false;
        }
        if (treatment == TreatmentOption.TOLERATE) {
            return targetResidualScore != null;
        }
        if (targetResidualScore == null) {
            return false;
        }
        return targetResidualScore > appetiteMaxResidual(gridSize, appetite);
    }

    public static String acceptanceRequiredLabel(TreatmentOption treatment, Integer targetResidualScore,
                                                MatrixGridSize gridSize, RiskLevel appetite) {
        if (treatment == TreatmentOption.TERMINATE || treatment == TreatmentOption.TRANSFER) {
            return "No";
        }
        if (treatment == TreatmentOption.TOLERATE) {
            return targetResidualScore == null ? null : "YES - Tolerate path";
        }
        if (targetResidualScore == null) {
            return null;
        }
        if (targetResidualScore <= appetiteMaxResidual(gridSize, appetite)) {
            return "No - plan reaches appetite";
        }
        return "YES - plan leaves it above appetite";
    }

    public static EvaluationDecision evaluation(MatrixGridSize gridSize, int residualScore, RiskLevel appetite) {
        if (autoClosable(gridSize, residualScore)) {
            return EvaluationDecision.AUTO_CLOSE;
        }
        if (residualScore <= appetiteMaxResidual(gridSize, appetite)) {
            return EvaluationDecision.MONITOR;
        }
        return EvaluationDecision.TREATMENT_REQUIRED;
    }

    public static RiskLevel workflowLevel(MatrixGridSize gridSize, int score) {
        return switch (rating(gridSize, score)) {
            case "CRITICAL", "VERY_HIGH" -> RiskLevel.VERY_HIGH;
            case "HIGH", "MED-HIGH" -> RiskLevel.HIGH;
            case "MEDIUM" -> RiskLevel.MEDIUM;
            case "LOW-MED", "LOW" -> RiskLevel.LOW;
            case "VERY_LOW" -> RiskLevel.VERY_LOW;
            default -> RiskLevel.MEDIUM;
        };
    }

    public static RiskLevel level(MatrixAxes axes, MatrixGridSize gridSize,
                                  RiskLevel probability, RiskLevel impact) {
        return workflowLevel(gridSize, score(axes, probability, impact));
    }

    public static int[][] scoreGrid(MatrixAxes axes) {
        RiskLevel[] rows = axes.rowLevels();
        RiskLevel[] cols = axes.colLevels();
        int[][] out = new int[rows.length][cols.length];
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                out[r][c] = score(axes, rows[r], cols[c]);
            }
        }
        return out;
    }

    public static RiskLevel[][] levelGrid(MatrixGridSize gridSize, MatrixAxes axes) {
        RiskLevel[] rows = axes.rowLevels();
        RiskLevel[] cols = axes.colLevels();
        RiskLevel[][] out = new RiskLevel[rows.length][cols.length];
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                out[r][c] = level(axes, gridSize, rows[r], cols[c]);
            }
        }
        return out;
    }

    /** @deprecated use {@link #autoClosable(MatrixGridSize, int)} from the Excel score */
    public static boolean autoClosable(RiskLevel residual) {
        return residual == RiskLevel.VERY_LOW;
    }
}
