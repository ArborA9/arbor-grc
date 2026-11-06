package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.TransferMethod;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitRiskRequest(
        @NotNull Boolean confirmTreatment,
        TreatmentOption treatmentOption,
        @Size(max = 8000) String justification,
        @Size(max = 8000) String comments,
        // Treat: optional plan details
        @Size(max = 8000) String actionDescription,
        Long actionOwnerId,
        Long custodianUserId,
        LocalDate startDate,
        LocalDate targetDate,
        @Size(max = 4000) String resourcesRequired,
        BigDecimal costEstimateAed,
        @Size(max = 4000) String evidenceRequired,
        // Tolerate: acceptance Section 2
        @Size(max = 8000) String acceptanceJustification,
        @Size(max = 8000) String businessRationale,
        @Size(max = 8000) String conditionsOfAcceptance,
        @Size(max = 8000) String compensatingControls,
        Long compControlOwnerId,
        RiskLevel compControlEffectiveness,
        LocalDate reviewDate,
        Boolean contributorDeclaration,
        // Terminate
        @Size(max = 8000) String eliminationJustification,
        @Size(max = 8000) String eliminationHow,
        Boolean activityStopped,
        // Transfer
        TransferMethod transferMethod,
        @Size(max = 512) String thirdPartyName,
        @Size(max = 8000) String transferDescription,
        @Size(max = 8000) String residualAfterTransfer,
        @Size(max = 8000) String transferCompensatingControls,
        @Size(max = 4000) String infosecStatusContributor,
        Long approver2UserId,
        Long approver3UserId,
        /** Acceptance form: contributor or admin may choose a different Risk Owner (Approver 1). */
        Long riskOwnerUserId
) {
}
