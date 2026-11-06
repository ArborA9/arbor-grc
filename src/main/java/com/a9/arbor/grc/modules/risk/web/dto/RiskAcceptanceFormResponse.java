package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.AcceptanceDecision;
import com.a9.arbor.grc.modules.risk.domain.AcceptanceFormStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskAcceptanceForm;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.TreatmentFormKind;

import java.time.Instant;
import java.time.LocalDate;

public record RiskAcceptanceFormResponse(
        Long id,
        Long riskId,
        String justification,
        String businessRationale,
        String conditionsOfAcceptance,
        String compensatingControls,
        Long compControlOwnerId,
        RiskLevel compControlEffectiveness,
        LocalDate reviewDate,
        boolean contributorDeclaration,
        AcceptanceDecision riskOwnerDecision,
        String riskOwnerComments,
        Instant riskOwnerReviewedAt,
        AcceptanceDecision adminDecision,
        String adminComments,
        Instant adminReviewedAt,
        AcceptanceFormStatus status,
        Instant createdAt,
        Instant updatedAt,
        TreatmentFormKind formKind,
        String displayId,
        int cycleNo,
        Long approver1UserId,
        Long approver2UserId,
        Long approver3UserId,
        AcceptanceDecision approver2Decision,
        String approver2Comments,
        Instant approver2ReviewedAt,
        AcceptanceDecision approver3Decision,
        String approver3Comments,
        Instant approver3ReviewedAt,
        Instant submittedAt,
        Long submittedByUserId,
        String approver1Name,
        String approver2Name,
        String approver3Name
) {
    public static RiskAcceptanceFormResponse from(RiskAcceptanceForm f) {
        return from(f, null, null, null);
    }

    public static RiskAcceptanceFormResponse from(RiskAcceptanceForm f,
                                                  String approver1Name,
                                                  String approver2Name,
                                                  String approver3Name) {
        return new RiskAcceptanceFormResponse(
                f.getId(),
                f.getRiskId(),
                f.getJustification(),
                f.getBusinessRationale(),
                f.getConditionsOfAcceptance(),
                f.getCompensatingControls(),
                f.getCompControlOwnerId(),
                f.getCompControlEffectiveness(),
                f.getReviewDate(),
                f.isContributorDeclaration(),
                f.getRiskOwnerDecision(),
                f.getRiskOwnerComments(),
                f.getRiskOwnerReviewedAt(),
                f.getAdminDecision(),
                f.getAdminComments(),
                f.getAdminReviewedAt(),
                f.getStatus(),
                f.getCreatedAt(),
                f.getUpdatedAt(),
                f.getFormKind(),
                f.getDisplayId(),
                f.getCycleNo(),
                f.getApprover1UserId(),
                f.getApprover2UserId(),
                f.getApprover3UserId(),
                f.getApprover2Decision(),
                f.getApprover2Comments(),
                f.getApprover2ReviewedAt(),
                f.getApprover3Decision(),
                f.getApprover3Comments(),
                f.getApprover3ReviewedAt(),
                f.getSubmittedAt(),
                f.getSubmittedByUserId(),
                approver1Name,
                approver2Name,
                approver3Name);
    }
}
