package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateRecoveryPlanRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull RecoveryPlanType planType,
        @Size(max = 4000) String scope,
        @Size(max = 8000) String crisisTeam,
        @Size(max = 8000) String escalationContacts,
        @Size(max = 8000) String communicationTree,
        @Size(max = 8000) String resourceRequirements,
        @Size(max = 8000) String alternateSiteDetails,
        Long ownerUserId,
        LocalDate reviewDate,
        List<Long> biaIds,
        List<RecoveryPlanProcedureRequest> procedures
) {
}
