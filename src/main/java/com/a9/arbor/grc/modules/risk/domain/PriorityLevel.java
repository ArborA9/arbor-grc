package com.a9.arbor.grc.modules.risk.domain;

/** Priority from Excel residual rating key (workbook RISK RATING KEY names). */
public enum PriorityLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW;

    public static PriorityLevel fromExcelRating(String rating) {
        if (rating == null) {
            return MEDIUM;
        }
        return switch (rating) {
            case "CRITICAL", "VERY_HIGH" -> CRITICAL;
            case "HIGH", "MED-HIGH" -> HIGH;
            case "MEDIUM" -> MEDIUM;
            case "LOW-MED", "LOW" -> LOW;
            case "VERY_LOW" -> LOW;
            default -> MEDIUM;
        };
    }

    /** Legacy bridge when Excel rating is unavailable. */
    public static PriorityLevel fromResidual(RiskLevel residual) {
        return switch (residual) {
            case VERY_HIGH -> CRITICAL;
            case HIGH -> HIGH;
            case MEDIUM -> MEDIUM;
            case LOW, VERY_LOW -> LOW;
        };
    }
}
