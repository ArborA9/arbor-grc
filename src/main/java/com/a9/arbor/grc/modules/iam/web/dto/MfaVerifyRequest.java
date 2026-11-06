package com.a9.arbor.grc.modules.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank String mfaToken,
        @NotBlank String code,
        Boolean rememberMe
) {
}
