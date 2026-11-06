package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.ResidualScoringMode;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import com.a9.arbor.grc.modules.risk.domain.UaeIaPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateRiskRequest(
        RiskType riskType,
        /** Optional Admin override of auto ISR-/ERM-YYYY-NNN (unique, logged). */
        @Size(max = 32) String displayIdOverride,
        @NotBlank @Size(max = 1024) String title,
        @Size(max = 8000) String description,
        @NotNull(message = "Risk Category is required") RiskCategory category,
        /** Required when category is OTHER. */
        @Size(max = 128) String categoryOther,
        Long orgUnitId,
        Long assetId,
        Long frameworkId,
        @Size(max = 64) String frameworkCode,
        Long frameworkNodeId,
        @Size(max = 64) String nodeCode,
        /** Optional manual UAE IA priority; otherwise auto from control when present. */
        UaeIaPriority uaeIaPriority,
        @Size(max = 8000) String observedGap,
        @Size(max = 8000) String threats,
        @NotNull RiskLevel probability,
        @NotNull RiskLevel impact,
        @Size(max = 8000) String existingControls,
        @NotNull RiskLevel controlEffectiveness,
        /** Doc 02 R-06: default CONTROL_EFFECTIVENESS. */
        ResidualScoringMode residualScoringMode,
        RiskLevel residualProbability,
        RiskLevel residualImpact,
        TreatmentOption treatmentOption,
        @Size(max = 8000) String treatmentPlan,
        @Valid TreatmentPlanDraftRequest treatmentPlanDetail,
        Long riskOwnerUserId,
        @Size(max = 8000) String adminNotes,
        @Size(max = 512) String strategicObjective,
        BigDecimal financialImpactAed,
        Long tprmVendorId,
        /** Keep open despite residual VL/L: requires justification (§13.3). */
        Boolean overrideAutoClose,
        @Size(max = 4000) String autoCloseOverrideJustification,
        @Valid List<RiskFrameworkMappingRequest> additionalFrameworks,
        Long sourceGapAssessmentId,
        Long sourceGapItemId,
        @Size(max = 4000) String infosecStatusInitial,
        Long riskProjectId,
        java.time.LocalDate dateIdentified,
        @Size(max = 128) String cause,
        Integer residualScore,
        RiskLevel targetControlEffectiveness,
        Long assignedContributorId,
        java.time.LocalDate nextReviewDate,
        @Size(max = 512) String thirdPartyName,
        @Size(max = 8000) String treatmentJustification,
        @Size(max = 8000) String compensatingControls,
        Long sourceDpiaId,
        Long sourceRopaId,
        Long sourceDpiaAssessmentRowId
) {
}
