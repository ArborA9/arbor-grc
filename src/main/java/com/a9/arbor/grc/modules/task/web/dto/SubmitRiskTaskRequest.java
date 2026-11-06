package com.a9.arbor.grc.modules.task.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.TransferMethod;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import com.a9.arbor.grc.modules.risk.web.dto.SubmitRiskRequest;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitRiskTaskRequest(
        @NotNull Boolean confirmTreatment,
        TreatmentOption treatmentOption,
        String justification,
        String comments,
        String actionDescription,
        Long actionOwnerId,
        Long custodianUserId,
        LocalDate startDate,
        LocalDate targetDate,
        String resourcesRequired,
        BigDecimal costEstimateAed,
        String evidenceRequired,
        String acceptanceJustification,
        String businessRationale,
        String conditionsOfAcceptance,
        String compensatingControls,
        Long compControlOwnerId,
        RiskLevel compControlEffectiveness,
        LocalDate reviewDate,
        Boolean contributorDeclaration,
        String eliminationJustification,
        String eliminationHow,
        Boolean activityStopped,
        TransferMethod transferMethod,
        String thirdPartyName,
        String transferDescription,
        String residualAfterTransfer,
        String transferCompensatingControls,
        String infosecStatusContributor,
        Long approver2UserId,
        Long approver3UserId,
        Long riskOwnerUserId
) {
    public SubmitRiskRequest toSubmitRiskRequest() {
        return new SubmitRiskRequest(
                confirmTreatment, treatmentOption, justification, comments,
                actionDescription, actionOwnerId, custodianUserId, startDate, targetDate, resourcesRequired,
                costEstimateAed, evidenceRequired,
                acceptanceJustification, businessRationale, conditionsOfAcceptance,
                compensatingControls, compControlOwnerId, compControlEffectiveness,
                reviewDate, contributorDeclaration,
                eliminationJustification, eliminationHow, activityStopped,
                transferMethod, thirdPartyName, transferDescription,
                residualAfterTransfer, transferCompensatingControls,
                infosecStatusContributor, approver2UserId, approver3UserId, riskOwnerUserId);
    }
}
