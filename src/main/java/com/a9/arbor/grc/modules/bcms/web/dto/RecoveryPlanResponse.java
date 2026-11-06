package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlan;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanStatus;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RecoveryPlanResponse(
        Long id,
        Long projectId,
        String name,
        RecoveryPlanType planType,
        String scope,
        int versionNum,
        RecoveryPlanStatus status,
        String crisisTeam,
        String escalationContacts,
        String communicationTree,
        String resourceRequirements,
        String alternateSiteDetails,
        Long ownerUserId,
        Long createdByUserId,
        Instant submittedAt,
        Instant approvedAt,
        Long approvedByUserId,
        LocalDate reviewDate,
        List<Long> biaIds,
        List<RecoveryPlanProcedureResponse> procedures,
        Instant createdAt,
        Instant updatedAt
) {
    public static RecoveryPlanResponse from(RecoveryPlan p, List<Long> biaIds,
                                            List<RecoveryPlanProcedureResponse> procedures) {
        return new RecoveryPlanResponse(
                p.getId(),
                p.getProjectId(),
                p.getName(),
                p.getPlanType(),
                p.getScope(),
                p.getVersionNum(),
                p.getStatus(),
                p.getCrisisTeam(),
                p.getEscalationContacts(),
                p.getCommunicationTree(),
                p.getResourceRequirements(),
                p.getAlternateSiteDetails(),
                p.getOwnerUserId(),
                p.getCreatedByUserId(),
                p.getSubmittedAt(),
                p.getApprovedAt(),
                p.getApprovedByUserId(),
                p.getReviewDate(),
                biaIds,
                procedures,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
