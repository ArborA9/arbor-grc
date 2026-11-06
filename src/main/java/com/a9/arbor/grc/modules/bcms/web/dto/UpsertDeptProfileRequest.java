package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertDeptProfileRequest(
        @NotNull Long orgUnitId,
        Integer totalStaff,
        @Size(max = 8000) String criticalStaffRoles,
        @Size(max = 8000) String primaryFunctions,
        @Size(max = 8000) String internalDependencies,
        @Size(max = 8000) String externalDependencies,
        @Size(max = 512) String physicalLocation,
        @Size(max = 512) String alternateWorkLocation,
        List<Long> assetIds
) {
}
