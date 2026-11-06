package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.MatrixGridSize;
import com.a9.arbor.grc.modules.risk.domain.ExcelRiskScoring;
import com.a9.arbor.grc.modules.risk.domain.EvaluationDecision;
import com.a9.arbor.grc.modules.risk.domain.FinalRiskStatus;
import com.a9.arbor.grc.modules.risk.domain.PriorityLevel;
import com.a9.arbor.grc.modules.risk.domain.ResidualScoringMode;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.domain.TransferMethod;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import com.a9.arbor.grc.modules.risk.domain.UaeIaPriority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RiskRecordResponse(
        Long id,
        RiskType riskType,
        String displayId,
        boolean displayIdOverridden,
        String title,
        String description,
        RiskCategory category,
        String categoryOther,
        RiskStatus status,
        PriorityLevel priorityLevel,
        UaeIaPriority uaeIaPriority,
        Long orgUnitId,
        Long assetId,
        Long frameworkId,
        String frameworkCode,
        Long frameworkNodeId,
        String nodeCode,
        String observedGap,
        String threats,
        RiskLevel probability,
        RiskLevel impact,
        RiskLevel inherentRisk,
        String existingControls,
        RiskLevel controlEffectiveness,
        ResidualScoringMode residualScoringMode,
        RiskLevel residualProbability,
        RiskLevel residualImpact,
        RiskLevel residualRisk,
        /** Client Excel: Probability × Impact numeric product. */
        Integer inherentScore,
        /** Client Excel RISK RATING KEY name (e.g. CRITICAL, MED-HIGH, LOW-MED). */
        String inherentRating,
        /** Client Excel RISK RATING KEY action text. */
        String inherentAction,
        Integer residualScore,
        String residualRating,
        String residualAction,
        boolean autoCloseOverride,
        String autoCloseOverrideJustification,
        TreatmentOption treatmentOption,
        String treatmentPlan,
        String treatmentChangeJustification,
        String eliminationJustification,
        String eliminationHow,
        Boolean activityStopped,
        TransferMethod transferMethod,
        String thirdPartyName,
        String transferDescription,
        String residualAfterTransfer,
        String transferCompensatingControls,
        String validationNotes,
        String rejectionReason,
        Instant validatedAt,
        Long validatedByUserId,
        LocalDate nextReviewDate,
        Long riskOwnerUserId,
        Long assignedContributorId,
        Long workTaskId,
        String adminNotes,
        String strategicObjective,
        BigDecimal financialImpactAed,
        Long tprmVendorId,
        Long sourceGapAssessmentId,
        Long sourceGapItemId,
        Long sourceDpiaId,
        Long sourceDpiaAssessmentRowId,
        Long sourceRopaId,
        Long sourceComplianceProjectId,
        Long sourceRiskTemplateId,
        Long createdByUserId,
        Instant assignedAt,
        Instant submittedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        TreatmentPlanResponse activeTreatmentPlan,
        RiskAcceptanceFormResponse acceptanceForm,
        List<RiskFrameworkMappingResponse> additionalFrameworks,
        String infosecStatusInitial,
        String infosecStatusContributor,
        String infosecStatusFinal,
        Long riskProjectId,
        LocalDate dateIdentified,
        String cause,
        java.math.BigDecimal riskReductionPct,
        Boolean withinAppetite,
        EvaluationDecision evaluationDecision,
        boolean acceptanceRequired,
        RiskLevel targetControlEffectiveness,
        Integer targetResidualScore,
        Boolean targetWithinAppetite,
        String custodianComments,
        String acceptanceRequiredLabel,
        String treatedRiskScore,
        FinalRiskStatus finalRiskStatus,
        boolean targetResidualManual,
        String orgUnitName,
        String riskOwnerName,
        String assignedContributorName
) {
    public static RiskRecordResponse from(RiskRecord risk,
                                          TreatmentPlanResponse plan,
                                          RiskAcceptanceFormResponse acceptance,
                                          List<RiskFrameworkMappingResponse> mappings) {
        return from(risk, plan, acceptance, mappings, null, null, null, null, null, null, null, null, null);
    }

    public static RiskRecordResponse from(RiskRecord risk,
                                          TreatmentPlanResponse plan,
                                          RiskAcceptanceFormResponse acceptance,
                                          List<RiskFrameworkMappingResponse> mappings,
                                          Integer inherentScore,
                                          String inherentRating,
                                          String inherentAction,
                                          Integer residualScore,
                                          String residualRating,
                                          String residualAction) {
        return from(risk, plan, acceptance, mappings, inherentScore, inherentRating, inherentAction,
                residualScore, residualRating, residualAction, null, null, null);
    }

    public static RiskRecordResponse from(RiskRecord risk,
                                          TreatmentPlanResponse plan,
                                          RiskAcceptanceFormResponse acceptance,
                                          List<RiskFrameworkMappingResponse> mappings,
                                          Integer inherentScore,
                                          String inherentRating,
                                          String inherentAction,
                                          Integer residualScore,
                                          String residualRating,
                                          String residualAction,
                                          String orgUnitName,
                                          String riskOwnerName,
                                          String assignedContributorName) {
        PriorityLevel priority = residualRating != null
                ? PriorityLevel.fromExcelRating(residualRating)
                : PriorityLevel.fromResidual(risk.getResidualRisk());
        String accLabel;
        TreatmentOption treat = risk.getTreatmentOption();
        if (treat == TreatmentOption.TERMINATE || treat == TreatmentOption.TRANSFER) {
            accLabel = "No";
        } else if (treat == TreatmentOption.TOLERATE) {
            accLabel = risk.getTargetResidualScore() == null ? null : "YES - Tolerate path";
        } else if (risk.getTargetResidualScore() == null) {
            accLabel = null;
        } else if (Boolean.TRUE.equals(risk.getTargetWithinAppetite())) {
            accLabel = "No - plan reaches appetite";
        } else {
            accLabel = "YES - plan leaves it above appetite";
        }
        String treatedBand = null;
        if (risk.getTargetResidualScore() != null && risk.getMatrixGridSize() != null) {
            try {
                treatedBand = ExcelRiskScoring.rating(
                        MatrixGridSize.fromInt(risk.getMatrixGridSize()), risk.getTargetResidualScore());
            } catch (Exception ignored) {
                treatedBand = null;
            }
        }
        return new RiskRecordResponse(
                risk.getId(),
                risk.getRiskType(),
                risk.getDisplayId(),
                risk.isDisplayIdOverridden(),
                risk.getTitle(),
                risk.getDescription(),
                risk.getCategory(),
                risk.getCategoryOther(),
                risk.getStatus(),
                priority,
                risk.getUaeIaPriority(),
                risk.getOrgUnitId(),
                risk.getAssetId(),
                risk.getFrameworkId(),
                risk.getFrameworkCode(),
                risk.getFrameworkNodeId(),
                risk.getNodeCode(),
                risk.getObservedGap(),
                risk.getThreats(),
                risk.getProbability(),
                risk.getImpact(),
                risk.getInherentRisk(),
                risk.getExistingControls(),
                risk.getControlEffectiveness(),
                risk.getResidualScoringMode() != null
                        ? risk.getResidualScoringMode()
                        : ResidualScoringMode.MANUAL_PROBABILITY_IMPACT,
                risk.getResidualProbability(),
                risk.getResidualImpact(),
                risk.getResidualRisk(),
                inherentScore,
                inherentRating,
                inherentAction,
                residualScore,
                residualRating,
                residualAction,
                risk.isAutoCloseOverride(),
                risk.getAutoCloseOverrideJustification(),
                risk.getTreatmentOption(),
                risk.getTreatmentPlan(),
                risk.getTreatmentChangeJustification(),
                risk.getEliminationJustification(),
                risk.getEliminationHow(),
                risk.getActivityStopped(),
                risk.getTransferMethod(),
                risk.getThirdPartyName(),
                risk.getTransferDescription(),
                risk.getResidualAfterTransfer(),
                risk.getTransferCompensatingControls(),
                risk.getValidationNotes(),
                risk.getRejectionReason(),
                risk.getValidatedAt(),
                risk.getValidatedByUserId(),
                risk.getNextReviewDate(),
                risk.getRiskOwnerUserId(),
                risk.getAssignedContributorId(),
                risk.getWorkTaskId(),
                risk.getAdminNotes(),
                risk.getStrategicObjective(),
                risk.getFinancialImpactAed(),
                risk.getTprmVendorId(),
                risk.getSourceGapAssessmentId(),
                risk.getSourceGapItemId(),
                risk.getSourceDpiaId(),
                risk.getSourceDpiaAssessmentRowId(),
                risk.getSourceRopaId(),
                risk.getSourceComplianceProjectId(),
                risk.getSourceRiskTemplateId(),
                risk.getCreatedByUserId(),
                risk.getAssignedAt(),
                risk.getSubmittedAt(),
                risk.getClosedAt(),
                risk.getCreatedAt(),
                risk.getUpdatedAt(),
                plan,
                acceptance,
                mappings == null ? List.of() : mappings,
                risk.getInfosecStatusInitial(),
                risk.getInfosecStatusContributor(),
                risk.getInfosecStatusFinal(),
                risk.getRiskProjectId(),
                risk.getDateIdentified(),
                risk.getCause(),
                risk.getRiskReductionPct(),
                risk.getWithinAppetite(),
                risk.getEvaluationDecision(),
                risk.isAcceptanceRequired(),
                risk.getTargetControlEffectiveness(),
                risk.getTargetResidualScore(),
                risk.getTargetWithinAppetite(),
                risk.getCustodianComments(),
                accLabel,
                treatedBand,
                risk.getFinalRiskStatus(),
                risk.isTargetResidualManual(),
                orgUnitName,
                riskOwnerName,
                assignedContributorName);
    }

    public static RiskRecordResponse from(RiskRecord risk) {
        return from(risk, null, null, List.of());
    }
}
