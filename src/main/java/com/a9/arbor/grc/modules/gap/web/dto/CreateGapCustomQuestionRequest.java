package com.a9.arbor.grc.modules.gap.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGapCustomQuestionRequest(
        @NotBlank @Size(max = 2000) String questionText
) {
}
