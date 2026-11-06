package com.a9.arbor.grc.modules.org.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrgProfileRequest(
        @Size(max = 200) String displayName,
        @Size(max = 100) String industry,
        @Size(max = 64) String sizeBand,
        @NotBlank @Size(max = 64) String timezone,
        @NotBlank @Size(max = 32) String dateFormat,
        /** Path C B3. Null keeps the current value (existing callers need not send it). */
        @Min(1) @Max(480) Integer idleTimeoutMinutes,
        /** BCMS RTO thresholds (hours) by BIA criticality. Null keeps the current value. */
        @Min(1) @Max(8760) Integer rtoMaxCriticalHours,
        @Min(1) @Max(8760) Integer rtoMaxHighHours,
        @Min(1) @Max(8760) Integer rtoMaxMediumHours,
        @Min(1) @Max(8760) Integer rtoMaxLowHours,
        @Min(3) @Max(5) Integer riskMatrixGridSize,
        @Size(max = 16) String riskAppetiteRating
) {
}
