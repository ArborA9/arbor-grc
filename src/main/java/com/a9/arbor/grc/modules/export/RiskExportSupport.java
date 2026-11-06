package com.a9.arbor.grc.modules.export;

import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared field layout for risk Excel / PDF export (Excel register row-5 names). */
public final class RiskExportSupport {

    private RiskExportSupport() {
    }

    static String[] registerHeaders() {
        return new String[]{
                "Risk ID",
                "Date Identified",
                "Risk Title",
                "Risk Description",
                "Gap / Observation",
                "Risk Category",
                "Specify category",
                "Division / Department",
                "Strategic Objective (Manual)",
                "Risk Cause / Source",
                "Control Ref",
                "Associated Threat",
                "Risk Owner",
                "Risk Custodian",
                "Likelihood",
                "Impact",
                "Inherent Risk Score",
                "Inherent Risk",
                "Existing Controls",
                "Control Effectiveness",
                "Risk Reduction %",
                "Residual Risk Score",
                "Residual Rating",
                "Within Appetite?",
                "Evaluation Decision",
                "Treatment Option",
                "Treatment Action Plan",
                "Risk Status",
                "Acceptance Required?",
                "Final Risk Status",
                "Target Residual Score",
                "Next Review Date",
        };
    }

    static String[] registerRow(RiskRecord r) {
        return new String[]{
                nullToEmpty(r.getDisplayId()),
                date(r.getDateIdentified()),
                nullToEmpty(r.getTitle()),
                nullToEmpty(r.getDescription()),
                nullToEmpty(r.getObservedGap()),
                prettyEnum(r.getCategory()),
                nullToEmpty(r.getCategoryOther()),
                r.getOrgUnitId() != null ? String.valueOf(r.getOrgUnitId()) : "",
                nullToEmpty(r.getStrategicObjective()),
                nullToEmpty(r.getCause()),
                nullToEmpty(r.getNodeCode()),
                nullToEmpty(r.getThreats()),
                r.getRiskOwnerUserId() != null ? String.valueOf(r.getRiskOwnerUserId()) : "",
                r.getAssignedContributorId() != null ? String.valueOf(r.getAssignedContributorId()) : "",
                prettyEnum(r.getProbability()),
                prettyEnum(r.getImpact()),
                num(r.getInherentScore()),
                prettyEnum(r.getInherentRisk()),
                nullToEmpty(r.getExistingControls()),
                prettyEnum(r.getControlEffectiveness()),
                r.getRiskReductionPct() != null ? r.getRiskReductionPct().stripTrailingZeros().toPlainString() : "",
                num(r.getResidualScore()),
                prettyEnum(r.getResidualRisk()),
                boolLabel(r.getWithinAppetite()),
                prettyEnum(r.getEvaluationDecision()),
                treatmentLabel(r.getTreatmentOption()),
                firstNonBlank(r.getTreatmentChangeJustification(), r.getTreatmentPlan()),
                prettyEnum(r.getStatus()),
                r.isAcceptanceRequired() ? "Yes" : "No",
                prettyEnum(r.getFinalRiskStatus()),
                num(r.getTargetResidualScore()),
                date(r.getNextReviewDate()),
        };
    }

    static List<String[]> detailRows(RiskRecord r) {
        List<String[]> rows = new ArrayList<>();
        add(rows, "Type", switch (r.getRiskType()) {
            case ERM -> "ERM";
            case PRIVACY_RISK -> "Data Privacy Risk";
            case IS_RISK -> "IS Risk";
        });
        add(rows, "Risk ID", r.getDisplayId());
        add(rows, "Date Identified", date(r.getDateIdentified()));
        add(rows, "Risk Title", r.getTitle());
        add(rows, "Risk Status", prettyEnum(r.getStatus()));
        add(rows, "Risk Description", r.getDescription());
        add(rows, "Gap / Observation", r.getObservedGap());
        add(rows, "Risk Category", prettyEnum(r.getCategory()));
        add(rows, "Specify category", r.getCategoryOther());
        add(rows, "Division / Department", r.getOrgUnitId());
        add(rows, "Strategic Objective (Manual)", r.getStrategicObjective());
        add(rows, "Risk Cause / Source", r.getCause());
        if (r.getRiskType() == RiskType.IS_RISK) {
            add(rows, "Control Ref", r.getNodeCode());
            add(rows, "Associated Threat", r.getThreats());
            add(rows, "Framework", r.getFrameworkCode());
            add(rows, "Asset ID", r.getAssetId());
        }
        add(rows, "Risk Owner", r.getRiskOwnerUserId());
        add(rows, "Risk Custodian", r.getAssignedContributorId());
        add(rows, "Likelihood", prettyEnum(r.getProbability()));
        add(rows, "Impact", prettyEnum(r.getImpact()));
        add(rows, "Inherent Risk Score", r.getInherentScore());
        add(rows, "Inherent Risk", prettyEnum(r.getInherentRisk()));
        add(rows, "Existing Controls", r.getExistingControls());
        add(rows, "Control Effectiveness", prettyEnum(r.getControlEffectiveness()));
        add(rows, "Risk Reduction %",
                r.getRiskReductionPct() != null ? r.getRiskReductionPct().stripTrailingZeros().toPlainString() : "");
        add(rows, "Residual Risk Score", r.getResidualScore());
        add(rows, "Residual Rating", prettyEnum(r.getResidualRisk()));
        add(rows, "Within Appetite?", boolLabel(r.getWithinAppetite()));
        add(rows, "Evaluation Decision", prettyEnum(r.getEvaluationDecision()));
        add(rows, "Treatment Option", treatmentLabel(r.getTreatmentOption()));
        add(rows, "Treatment Action Plan",
                firstNonBlank(r.getTreatmentChangeJustification(), r.getTreatmentPlan()));
        add(rows, "Acceptance Required?", r.isAcceptanceRequired() ? "Yes" : "No");
        add(rows, "Final Risk Status", prettyEnum(r.getFinalRiskStatus()));
        add(rows, "Target Residual Score", r.getTargetResidualScore());
        add(rows, "Next Review Date", date(r.getNextReviewDate()));
        add(rows, "Created at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
        add(rows, "Updated at", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : "");
        add(rows, "Submitted at", r.getSubmittedAt() != null ? r.getSubmittedAt().toString() : "");
        return rows;
    }

    static String documentTitle(RiskRecord r) {
        return switch (r.getRiskType()) {
            case ERM -> "ERM risk";
            case PRIVACY_RISK -> "Data Privacy Risk";
            case IS_RISK -> "IS risk";
        };
    }

    public static String filenameStem(RiskRecord r) {
        String prefix = switch (r.getRiskType()) {
            case ERM -> "erm-risk";
            case PRIVACY_RISK -> "data-privacy-risk";
            case IS_RISK -> "is-risk";
        };
        String id = r.getDisplayId() != null && !r.getDisplayId().isBlank()
                ? r.getDisplayId()
                : "risk-" + r.getId();
        return prefix + "-" + id.replaceAll("[^A-Za-z0-9._-]+", "-");
    }

    private static void add(List<String[]> rows, String field, String value) {
        rows.add(new String[]{field, value != null ? value : ""});
    }

    private static void add(List<String[]> rows, String field, Object value) {
        add(rows, field, value != null ? String.valueOf(value) : "");
    }

    private static String treatmentLabel(TreatmentOption option) {
        if (option == null) {
            return "";
        }
        return switch (option) {
            case TREAT -> "Treat (Mitigate)";
            case TOLERATE -> "Tolerate (Accept)";
            case TERMINATE -> "Terminate (Avoid)";
            case TRANSFER -> "Transfer";
        };
    }

    private static String prettyEnum(Enum<?> value) {
        if (value == null) {
            return "";
        }
        String raw = value.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        String[] parts = raw.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }

    private static String boolLabel(Boolean value) {
        if (value == null) {
            return "";
        }
        return value ? "Yes" : "No";
    }

    private static String date(java.time.LocalDate value) {
        return value != null ? value.toString() : "";
    }

    private static String num(Integer value) {
        return value != null ? String.valueOf(value) : "";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b != null ? b : "";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
