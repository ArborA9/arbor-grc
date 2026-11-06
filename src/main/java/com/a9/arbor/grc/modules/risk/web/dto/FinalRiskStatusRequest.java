package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.FinalRiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FinalRiskStatusRequest(
        @NotNull FinalRiskStatus decision,
        @Size(max = 4000) String reason,
        /** Excel AK Action — Config Risk Status. Required on Approve for project risks. */
        RiskStatus actionStatus,
        /** Excel AL Next Review Date. Required on Approve for project risks. */
        LocalDate nextReviewDate,
        /**
         * When true, Reject returns the risk to the custodian even if Acceptance Required is YES.
         * Absent or false does not start the acceptance journey.
         */
        Boolean sendBack,
        /**
         * Only the Risk Acceptance action sets this. Send Back must leave it false.
         * Acceptance Required YES does not open the acceptance sheet by itself.
         */
        Boolean startAcceptance
) {
}
