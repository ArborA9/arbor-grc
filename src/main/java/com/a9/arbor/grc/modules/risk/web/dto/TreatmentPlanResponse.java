package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.TreatmentMilestone;
import com.a9.arbor.grc.modules.risk.domain.TreatmentPlan;
import com.a9.arbor.grc.modules.risk.domain.TreatmentPlanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TreatmentPlanResponse(
        Long id,
        Long riskId,
        String actionDescription,
        Long actionOwnerId,
        Long custodianUserId,
        LocalDate startDate,
        LocalDate targetDate,
        String resourcesRequired,
        BigDecimal costEstimateAed,
        String evidenceRequired,
        TreatmentPlanStatus status,
        LocalDate completionDate,
        Instant createdAt,
        Instant updatedAt,
        List<TreatmentMilestoneResponse> milestones
) {
    public static TreatmentPlanResponse from(TreatmentPlan plan, List<TreatmentMilestone> milestones) {
        return new TreatmentPlanResponse(
                plan.getId(),
                plan.getRiskId(),
                plan.getActionDescription(),
                plan.getActionOwnerId(),
                plan.getCustodianUserId(),
                plan.getStartDate(),
                plan.getTargetDate(),
                plan.getResourcesRequired(),
                plan.getCostEstimateAed(),
                plan.getEvidenceRequired(),
                plan.getStatus(),
                plan.getCompletionDate(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                milestones.stream().map(TreatmentMilestoneResponse::from).toList());
    }
}
