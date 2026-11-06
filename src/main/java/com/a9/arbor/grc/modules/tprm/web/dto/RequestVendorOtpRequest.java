package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestVendorOtpRequest(
        @NotBlank String tenantId,
        @NotBlank String token,
        @NotBlank @Email String email
) {
}
