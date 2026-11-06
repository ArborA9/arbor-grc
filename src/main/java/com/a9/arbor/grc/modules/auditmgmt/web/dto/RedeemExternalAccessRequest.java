package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemExternalAccessRequest(
        @NotBlank String tenantId,
        @NotBlank String token,
        @NotBlank String otp
) {
}
