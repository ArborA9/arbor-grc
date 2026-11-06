package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTprmProjectRequest(
        @NotNull Long vendorId,
        @NotNull Long questionnaireId
) {
}
