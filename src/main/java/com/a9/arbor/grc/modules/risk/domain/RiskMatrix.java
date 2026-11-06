package com.a9.arbor.grc.modules.risk.domain;

/**
 * Default inherent/residual matrices from the client Excel workbook
 * ({@link ExcelRiskScoring}: P×I product + rating bands). Tenant axis size
 * lives in {@code risk_matrix_config}; cell scoring always follows Excel math.
 */
public final class RiskMatrix {

    /** §13.3 Control effectiveness × Inherent (auto residual path). */
    private static final RiskLevel[][] RESIDUAL_CE = {
            // inherent: VL, L, M, H, VH: rows: control effectiveness VH, H, M, L, VL
            {RiskLevel.VERY_LOW, RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.LOW, RiskLevel.LOW},
            {RiskLevel.VERY_LOW, RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.MEDIUM},
            {RiskLevel.VERY_LOW, RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH},
            {RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH},
            {RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH},
    };

    private RiskMatrix() {
    }

    /** Excel-derived inherent grid (Probability × Impact bands). */
    public static RiskLevel[][] defaultInherent() {
        return defaultInherent(MatrixGridSize.FIVE, MatrixAxes.defaults(MatrixGridSize.FIVE));
    }

    /** Excel residual P×I grid (manual residual path: same math as inherent). */
    public static RiskLevel[][] defaultResidual() {
        return defaultResidual(MatrixGridSize.FIVE, MatrixAxes.defaults(MatrixGridSize.FIVE));
    }

    /** Classic CE × Inherent grid for auto residual. */
    public static RiskLevel[][] defaultResidualCe() {
        return copyOf(RESIDUAL_CE);
    }

    public static RiskLevel inherent(RiskLevel probability, RiskLevel impact) {
        return inherent(MatrixGridSize.FIVE, probability, impact);
    }

    /** Auto residual: Control effectiveness × Inherent (§13.3). */
    public static RiskLevel residual(RiskLevel controlEffectiveness, RiskLevel inherent) {
        return residual(MatrixGridSize.FIVE, controlEffectiveness, inherent);
    }

    /** Extract a smaller grid from a 5×5 matrix using the given axis levels. */
    public static RiskLevel[][] projectGrid(RiskLevel[][] full5x5, MatrixAxes axes) {
        RiskLevel[] rows = axes.rowLevels();
        RiskLevel[] cols = axes.colLevels();
        RiskLevel[][] out = new RiskLevel[rows.length][cols.length];
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                out[r][c] = full5x5[rowOf(rows[r])][colOf(cols[c])];
            }
        }
        return out;
    }

    /** Extract a smaller grid from a 5×5 matrix using active axis levels. */
    public static RiskLevel[][] projectGrid(RiskLevel[][] full5x5, MatrixGridSize target) {
        if (target == MatrixGridSize.FIVE) {
            return copyOf(full5x5);
        }
        return projectGrid(full5x5, MatrixAxes.defaults(target));
    }

    public static RiskLevel levelAt(RiskLevel[][] grid, MatrixAxes axes, RiskLevel rowLevel, RiskLevel colLevel) {
        int r = axes.rowIndex(rowLevel);
        int c = axes.colIndex(colLevel);
        if (r < 0 || c < 0 || r >= grid.length || c >= grid[r].length) {
            throw new IllegalArgumentException("Level outside active matrix axes");
        }
        return grid[r][c];
    }

    public static RiskLevel inherent(MatrixGridSize gridSize, RiskLevel probability, RiskLevel impact) {
        return probabilityImpact(gridSize, MatrixAxes.defaults(gridSize), probability, impact);
    }

    public static RiskLevel residual(MatrixGridSize gridSize, RiskLevel controlEffectiveness, RiskLevel inherent) {
        return residual(gridSize, MatrixAxes.defaults(gridSize), controlEffectiveness, inherent);
    }

    public static RiskLevel[][] defaultInherent(MatrixGridSize gridSize) {
        return defaultInherent(gridSize, MatrixAxes.defaults(gridSize));
    }

    public static RiskLevel[][] defaultResidual(MatrixGridSize gridSize) {
        return defaultResidual(gridSize, MatrixAxes.defaults(gridSize));
    }

    public static RiskLevel[][] defaultResidualCe(MatrixGridSize gridSize) {
        return defaultResidualCe(gridSize, MatrixAxes.defaults(gridSize));
    }

    public static RiskLevel levelAt(RiskLevel[][] grid, RiskLevel rowLevel, RiskLevel colLevel) {
        return levelAt(grid, MatrixGridSize.FIVE, rowLevel, colLevel);
    }

    public static RiskLevel levelAt(RiskLevel[][] grid, MatrixGridSize gridSize, RiskLevel rowLevel, RiskLevel colLevel) {
        return levelAt(grid, MatrixAxes.defaults(gridSize), rowLevel, colLevel);
    }

    /** Client Excel: Probability × Impact → numeric score → rating band. */
    public static RiskLevel probabilityImpact(MatrixGridSize gridSize, MatrixAxes axes,
                                              RiskLevel probability, RiskLevel impact) {
        return ExcelRiskScoring.level(axes, gridSize, probability, impact);
    }

    public static RiskLevel inherent(MatrixGridSize gridSize, MatrixAxes axes, RiskLevel probability, RiskLevel impact) {
        return probabilityImpact(gridSize, axes, probability, impact);
    }

    /**
     * Named 5×5 lookup (Control effectiveness rows × Inherent columns), independent of
     * tenant axis order and of Excel P×I collapse. Matches §13.3 / client residual matrix.
     */
    public static RiskLevel residual(MatrixGridSize gridSize, MatrixAxes axes, RiskLevel controlEffectiveness, RiskLevel inherent) {
        return residualFromControlEffectiveness(controlEffectiveness, inherent);
    }

    public static RiskLevel residualFromControlEffectiveness(RiskLevel controlEffectiveness, RiskLevel inherent) {
        return RESIDUAL_CE[rowOf(controlEffectiveness)][colOf(inherent)];
    }

    public static RiskLevel inherent(RiskLevel[][] grid, MatrixAxes axes, RiskLevel probability, RiskLevel impact) {
        MatrixGridSize size = MatrixGridSize.fromInt(axes.rowLevels().length);
        return probabilityImpact(size, axes, probability, impact);
    }

    public static RiskLevel residual(RiskLevel[][] grid, MatrixAxes axes, RiskLevel controlEffectiveness, RiskLevel inherent) {
        return levelAt(grid, axes, controlEffectiveness, inherent);
    }

    public static RiskLevel[][] defaultInherent(MatrixGridSize gridSize, MatrixAxes axes) {
        return ExcelRiskScoring.levelGrid(gridSize, axes);
    }

    /** Excel residual = same P×I product grid as inherent (manual path). */
    public static RiskLevel[][] defaultResidual(MatrixGridSize gridSize, MatrixAxes axes) {
        return ExcelRiskScoring.levelGrid(gridSize, axes);
    }

    /** Auto path: CE × Inherent (§13.3), projected to active axes. */
    public static RiskLevel[][] defaultResidualCe(MatrixGridSize gridSize, MatrixAxes axes) {
        if (gridSize == MatrixGridSize.FIVE && MatrixAxes.defaults(gridSize).equals(axes)) {
            return copyOf(RESIDUAL_CE);
        }
        return projectGrid(RESIDUAL_CE, axes);
    }

    public static boolean autoClosable(RiskLevel residual) {
        return ExcelRiskScoring.autoClosable(residual);
    }

    /** Row index: VH → VL (matches matrix row order in §13.3). */
    public static int rowOf(RiskLevel level) {
        return switch (level) {
            case VERY_HIGH -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
            case VERY_LOW -> 4;
        };
    }

    /** Column index: VL → VH (matches matrix column order in §13.3). */
    public static int colOf(RiskLevel level) {
        return switch (level) {
            case VERY_LOW -> 0;
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case VERY_HIGH -> 4;
        };
    }

    private static RiskLevel[][] copyOf(RiskLevel[][] src) {
        RiskLevel[][] copy = new RiskLevel[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }
}
