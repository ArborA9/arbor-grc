package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBiaProjectRequest(
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotBlank @Size(max = 255) String name
) {
}
