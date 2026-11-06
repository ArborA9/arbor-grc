package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTprmProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull Long vendorId,
        @NotNull Long questionnaireId
) {
}
