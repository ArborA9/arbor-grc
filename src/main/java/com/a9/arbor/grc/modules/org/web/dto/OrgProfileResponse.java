package com.a9.arbor.grc.modules.org.web.dto;

import com.a9.arbor.grc.modules.org.domain.OrgProfile;

import java.time.Instant;

public record OrgProfileResponse(
        Long id,
        String displayName,
        String industry,
        String sizeBand,
        String timezone,
        String dateFormat,
        boolean wizardCompleted,
        Instant wizardCompletedAt,
        int idleTimeoutMinutes,
        int rtoMaxCriticalHours,
        int rtoMaxHighHours,
        int rtoMaxMediumHours,
        int rtoMaxLowHours,
        int riskMatrixGridSize,
        boolean riskMatrixLocked,
        String riskAppetiteRating,
        Instant updatedAt
) {
    public static OrgProfileResponse from(OrgProfile p) {
        return new OrgProfileResponse(
                p.getId(),
                p.getDisplayName(),
                p.getIndustry(),
                p.getSizeBand(),
                p.getTimezone(),
                p.getDateFormat(),
                p.isWizardCompleted(),
                p.getWizardCompletedAt(),
                p.getIdleTimeoutMinutes(),
                p.getRtoMaxCriticalHours(),
                p.getRtoMaxHighHours(),
                p.getRtoMaxMediumHours(),
                p.getRtoMaxLowHours(),
                p.getRiskMatrixGridSize(),
                p.isRiskMatrixLocked(),
                p.getRiskAppetiteRating(),
                p.getUpdatedAt());
    }
}
