package com.a9.arbor.grc.catalog.web.dto;

/** Per-framework leaf counts by routing flag (activation / library UI). */
public record FrameworkRoutingCounts(
        int inSoa,
        int inGapAssessment,
        int inAuditChecklist,
        int inRiskRegister
) {
    public static final FrameworkRoutingCounts NONE = new FrameworkRoutingCounts(0, 0, 0, 0);
}
