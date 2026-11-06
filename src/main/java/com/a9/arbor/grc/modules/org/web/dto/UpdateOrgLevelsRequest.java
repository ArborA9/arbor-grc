package com.a9.arbor.grc.modules.org.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateOrgLevelsRequest(
        @NotEmpty @Valid List<LevelLabel> levels
) {
    public record LevelLabel(
            @NotNull @Min(1) @Max(4) Integer level,
            @NotBlank @Size(max = 64) String label
    ) {
    }
}
