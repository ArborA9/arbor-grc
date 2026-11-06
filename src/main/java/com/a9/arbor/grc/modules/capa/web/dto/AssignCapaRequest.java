package com.a9.arbor.grc.modules.capa.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignCapaRequest(
        @NotNull Long ownerUserId,
        LocalDate targetDate,
        String contributorDepartment
) {
}
