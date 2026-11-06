package com.a9.arbor.grc.modules.risk.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tenant-chosen severity levels for matrix row (high → low) and column (low → high) axes.
 * Cell lookup is by index into these arrays, not by fixed {@link MatrixGridSize} presets.
 */
public record MatrixAxes(RiskLevel[] rowLevels, RiskLevel[] colLevels) {

    public MatrixAxes {
        rowLevels = rowLevels != null ? rowLevels.clone() : new RiskLevel[0];
        colLevels = colLevels != null ? colLevels.clone() : new RiskLevel[0];
    }

    public static MatrixAxes defaults(MatrixGridSize gridSize) {
        return new MatrixAxes(gridSize.axisRows(), gridSize.axisCols());
    }

    public int size() {
        return rowLevels.length;
    }

    public void validateFor(MatrixGridSize gridSize) {
        int n = gridSize.size();
        if (rowLevels.length != n || colLevels.length != n) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Axis levels must have exactly " + n + " entries for a " + n + "×" + n + " matrix");
        }
        validateDistinct(rowLevels, "Row axis");
        validateDistinct(colLevels, "Column axis");
    }

    private static void validateDistinct(RiskLevel[] levels, String label) {
        Set<RiskLevel> seen = new HashSet<>();
        for (RiskLevel level : levels) {
            if (level == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " contains a null level");
            }
            if (!seen.add(level)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        label + " levels must be distinct (duplicate " + level + ")");
            }
        }
    }

    public boolean isActive(RiskLevel level) {
        if (level == null) {
            return false;
        }
        for (RiskLevel row : rowLevels) {
            if (row == level) {
                return true;
            }
        }
        for (RiskLevel col : colLevels) {
            if (col == level) {
                return true;
            }
        }
        return false;
    }

    public void requireActive(RiskLevel level, String field) {
        if (!isActive(level)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " \"" + MatrixGridSize.displayName(level) + "\" is not on this organisation's risk matrix");
        }
    }

    public int rowIndex(RiskLevel level) {
        for (int i = 0; i < rowLevels.length; i++) {
            if (rowLevels[i] == level) {
                return i;
            }
        }
        return -1;
    }

    public int colIndex(RiskLevel level) {
        for (int i = 0; i < colLevels.length; i++) {
            if (colLevels[i] == level) {
                return i;
            }
        }
        return -1;
    }

    /** Map a stored level onto the nearest row axis level (heat-map aggregation). */
    public RiskLevel clampRow(RiskLevel level) {
        return clampToAxis(level, rowLevels);
    }

    /** Map a stored level onto the nearest column axis level (heat-map aggregation). */
    public RiskLevel clampCol(RiskLevel level) {
        return clampToAxis(level, colLevels);
    }

    private static RiskLevel clampToAxis(RiskLevel level, RiskLevel[] axis) {
        if (level == null || axis.length == 0) {
            return axis.length > 0 ? axis[axis.length / 2] : RiskLevel.MEDIUM;
        }
        for (RiskLevel a : axis) {
            if (a == level) {
                return level;
            }
        }
        int target = ordinal(level);
        RiskLevel best = axis[0];
        int bestDist = Integer.MAX_VALUE;
        for (RiskLevel a : axis) {
            int d = Math.abs(ordinal(a) - target);
            if (d < bestDist) {
                bestDist = d;
                best = a;
            }
        }
        return best;
    }

    private static int ordinal(RiskLevel level) {
        return switch (level) {
            case VERY_LOW -> 0;
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case VERY_HIGH -> 4;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MatrixAxes other)) {
            return false;
        }
        return Arrays.equals(rowLevels, other.rowLevels) && Arrays.equals(colLevels, other.colLevels);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rowLevels) * 31 + Arrays.hashCode(colLevels);
    }
}
