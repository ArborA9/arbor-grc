package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemVendorAccessRequest(
        @NotBlank String tenantId,
        @NotBlank String token,
        @NotBlank String otp
) {
}
