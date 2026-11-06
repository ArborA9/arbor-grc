package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBiaCycleRequest(
        @NotNull Long biaProjectId,
        @NotNull Long orgUnitId,
        String displayId,
        LocalDate issueDate,
        LocalDate nextReviewDate,
        String versionLabel,
        String objective,
        String scopeText,
        String mbco
) {
}
