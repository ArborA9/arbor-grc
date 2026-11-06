package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.ResidualScoringMode;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import com.a9.arbor.grc.modules.risk.domain.UaeIaPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateRiskRequest(
        @Size(max = 1024) String title,
        @Size(max = 8000) String description,
        RiskCategory category,
        @Size(max = 128) String categoryOther,
        Long orgUnitId,
        Long assetId,
        Long frameworkId,
        @Size(max = 64) String frameworkCode,
        Long frameworkNodeId,
        @Size(max = 64) String nodeCode,
        UaeIaPriority uaeIaPriority,
        @Size(max = 8000) String observedGap,
        @Size(max = 8000) String threats,
        RiskLevel probability,
        RiskLevel impact,
        @Size(max = 8000) String existingControls,
        RiskLevel controlEffectiveness,
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
        Boolean overrideAutoClose,
        @Size(max = 4000) String autoCloseOverrideJustification,
        @Valid List<RiskFrameworkMappingRequest> additionalFrameworks,
        /** Optional reason when Probability / Impact / CE change (§13.7 score history). */
        @Size(max = 1000) String scoreChangeReason,
        java.time.LocalDate dateIdentified,
        @Size(max = 128) String cause,
        Integer residualScore,
        RiskLevel targetControlEffectiveness,
        Long assignedContributorId,
        java.time.LocalDate nextReviewDate,
        @Size(max = 512) String thirdPartyName,
        @Size(max = 8000) String treatmentJustification,
        @Size(max = 8000) String compensatingControls
) {
}
