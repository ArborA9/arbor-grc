package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpsertBiaProcessRequest(
        @NotBlank String processCode,
        @NotBlank String processName,
        String description,
        String flowTrigger,
        String flowSteps,
        String flowOutput,
        String thirdPartyActivities,
        String supplierName,
        Long ownerOrgUnitId,
        Long ownerUserId,
        String frequency,
        String peakPeriod,
        String depStaff,
        String depPremises,
        String depIt,
        String depEquipment,
        String depRecords,
        String depThirdParty,
        String relatedRiskCode,
        String championNotes,
        List<BiaPersonRef> depStaffPeople,
        List<BiaAssetDependency> depAssets
) {
}
