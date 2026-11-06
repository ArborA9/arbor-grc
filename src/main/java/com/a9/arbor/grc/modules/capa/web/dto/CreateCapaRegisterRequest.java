package com.a9.arbor.grc.modules.capa.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCapaRegisterRequest(
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotBlank String name
) {
}
