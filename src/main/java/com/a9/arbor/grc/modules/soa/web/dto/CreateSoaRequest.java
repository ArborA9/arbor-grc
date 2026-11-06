package com.a9.arbor.grc.modules.soa.web.dto;

import jakarta.validation.constraints.NotNull;

public record CreateSoaRequest(
        @NotNull Long projectId,
        @NotNull Long frameworkId
) {
}
