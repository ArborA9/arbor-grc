package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewBiaProcessRequest(
        @NotBlank String outcome,
        String comment
) {
}
