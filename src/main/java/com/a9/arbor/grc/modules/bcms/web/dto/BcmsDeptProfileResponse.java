package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.BcmsDeptProfile;

import java.time.Instant;
import java.util.List;

public record BcmsDeptProfileResponse(
        Long id,
        Long orgUnitId,
        Integer totalStaff,
        String criticalStaffRoles,
        String primaryFunctions,
        String internalDependencies,
        String externalDependencies,
        String physicalLocation,
        String alternateWorkLocation,
        List<Long> assetIds,
        Instant createdAt,
        Instant updatedAt
) {
    public static BcmsDeptProfileResponse from(BcmsDeptProfile p, List<Long> assetIds) {
        return new BcmsDeptProfileResponse(
                p.getId(),
                p.getOrgUnitId(),
                p.getTotalStaff(),
                p.getCriticalStaffRoles(),
                p.getPrimaryFunctions(),
                p.getInternalDependencies(),
                p.getExternalDependencies(),
                p.getPhysicalLocation(),
                p.getAlternateWorkLocation(),
                assetIds,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
