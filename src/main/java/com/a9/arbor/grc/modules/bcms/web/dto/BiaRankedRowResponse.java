package com.a9.arbor.grc.modules.bcms.web.dto;

public record BiaRankedRowResponse(
        int recoveryOrder,
        Long processId,
        String processName,
        Long ownerOrgUnitId,
        String categoryLabel,
        Integer rtoHours,
        Integer maoHours,
        Integer capacityPct,
        Integer minStaff,
        String recoveryStrategy
) {
}
