package com.a9.arbor.grc.modules.export;

import java.util.List;

/**
 * Figures for the Executive GRC Summary PDF. Empty figures use "-".
 * NCEMA assessments are already excluded by the caller.
 */
public record ExecutiveSummaryBrief(
        String overallCompliance,
        String uaeIaScore,
        String uaeIaCode,
        String openIsRisks,
        String openErmRisks,
        String criticalRisks,
        String overdueTreatments,
        String openCapas,
        String overdueCapas,
        String openFindings,
        String highRiskVendors,
        String biaApproved,
        List<FrameworkScore> frameworks) {

    public ExecutiveSummaryBrief {
        frameworks = frameworks == null ? List.of() : List.copyOf(frameworks);
    }

    public record FrameworkScore(String code, String name, String score) {
    }
}
