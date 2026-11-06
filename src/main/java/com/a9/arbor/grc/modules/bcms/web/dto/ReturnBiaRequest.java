package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnBiaRequest(
        @NotBlank @Size(max = 2000) String reason
) {
}
