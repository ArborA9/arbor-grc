package com.a9.arbor.grc.modules.risk.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tenant risk matrix dimensions. Row/col axes are subsets of {@link RiskLevel};
 * 4×4 drops Very High (symmetric); 3×3 uses Low · Medium · High only.
 */
public enum MatrixGridSize {
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int size;

    MatrixGridSize(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

    public static MatrixGridSize fromInt(int n) {
        return switch (n) {
            case 3 -> THREE;
            case 4 -> FOUR;
            case 5 -> FIVE;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "gridSize must be 3, 4, or 5");
        };
    }

    /** Matrix row levels top → bottom (high probability / high CE at top). */
    public RiskLevel[] axisRows() {
        return switch (this) {
            case THREE -> new RiskLevel[]{RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW};
            case FOUR -> new RiskLevel[]{
                    RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW, RiskLevel.VERY_LOW
            };
            case FIVE -> new RiskLevel[]{
                    RiskLevel.VERY_HIGH, RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW, RiskLevel.VERY_LOW
            };
        };
    }

    /** Matrix column levels left → right. */
    public RiskLevel[] axisCols() {
        return switch (this) {
            case THREE -> new RiskLevel[]{RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH};
            case FOUR -> new RiskLevel[]{
                    RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH
            };
            case FIVE -> new RiskLevel[]{
                    RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH
            };
        };
    }

    public boolean isActive(RiskLevel level) {
        for (RiskLevel row : axisRows()) {
            if (row == level) {
                return true;
            }
        }
        for (RiskLevel col : axisCols()) {
            if (col == level) {
                return true;
            }
        }
        return false;
    }

    public int rowIndex(RiskLevel level) {
        RiskLevel clamped = clampRow(level);
        RiskLevel[] rows = axisRows();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == clamped) {
                return i;
            }
        }
        return -1;
    }

    public int colIndex(RiskLevel level) {
        RiskLevel clamped = clampCol(level);
        RiskLevel[] cols = axisCols();
        for (int i = 0; i < cols.length; i++) {
            if (cols[i] == clamped) {
                return i;
            }
        }
        return -1;
    }

    /** Map legacy Very High onto High when tenant uses 4×4 (heat-map aggregation only). */
    public RiskLevel clampRow(RiskLevel level) {
        if (this == FOUR && level == RiskLevel.VERY_HIGH) {
            return RiskLevel.HIGH;
        }
        if (this == THREE) {
            if (level == RiskLevel.VERY_HIGH || level == RiskLevel.HIGH) {
                return RiskLevel.HIGH;
            }
            if (level == RiskLevel.VERY_LOW) {
                return RiskLevel.LOW;
            }
        }
        return level;
    }

    public RiskLevel clampCol(RiskLevel level) {
        if (this == FOUR && level == RiskLevel.VERY_HIGH) {
            return RiskLevel.HIGH;
        }
        if (this == THREE) {
            if (level == RiskLevel.VERY_HIGH || level == RiskLevel.HIGH) {
                return RiskLevel.HIGH;
            }
            if (level == RiskLevel.VERY_LOW) {
                return RiskLevel.LOW;
            }
        }
        return level;
    }

    public void requireActive(RiskLevel level, String field) {
        if (!isActive(level)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " \"" + displayName(level) + "\" is not valid for a " + size + "x" + size + " matrix");
        }
    }

    static String displayName(RiskLevel level) {
        if (level == null) {
            return "-";
        }
        String s = level.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder();
        for (String w : s.split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return out.toString();
    }
}
