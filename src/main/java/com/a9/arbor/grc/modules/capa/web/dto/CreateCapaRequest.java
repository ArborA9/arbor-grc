package com.a9.arbor.grc.modules.capa.web.dto;

import com.a9.arbor.grc.modules.capa.domain.CapaPriority;
import com.a9.arbor.grc.modules.capa.domain.CapaRegister;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateCapaRequest(
        @NotNull CapaRegister registerCode,
        @NotBlank String title,
        String description,
        String recommendedAction,
        String controlRef,
        String severity,
        @NotNull CapaPriority priority,
        LocalDate targetDate,
        Long auditId,
        Long capaRegisterId,
        Long findingId,
        String externalFindingCode,
        /** Required when registerCode is OTHER. */
        String sourceOther,
        String conductedBy,
        String correction,
        String rootCause,
        String correctiveActionPlan,
        String evidenceRequired,
        String contributorDepartment,
        Long assignedContributorUserId
) {
}
