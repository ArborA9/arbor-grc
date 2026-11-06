package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameSheetTabRequest(
        @NotBlank @Size(max = 128) String fromLabel,
        @NotBlank @Size(max = 128) String toLabel
) {
}
