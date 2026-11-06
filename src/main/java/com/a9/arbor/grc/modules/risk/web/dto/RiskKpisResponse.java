package com.a9.arbor.grc.modules.risk.web.dto;

/** Open-risk residual band counts for the project picker. Same bands as the register KPIs. */
public record RiskKpisResponse(
        int veryHigh,
        int high,
        int medium,
        int low,
        int veryLow,
        int totalOpen
) {
}
