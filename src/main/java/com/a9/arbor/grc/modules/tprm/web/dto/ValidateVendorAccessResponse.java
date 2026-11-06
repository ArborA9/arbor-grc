package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.VendorTokenStatus;

import java.time.Instant;

public record ValidateVendorAccessResponse(
        boolean valid,
        VendorTokenStatus status,
        String vendorName,
        String assessmentDisplayId,
        Instant expiresAt,
        String message,
        String previewOtp
) {
}
