package com.a9.arbor.grc.modules.privacy.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateInventoryRequest(
        @Size(max = 255) String dataType,
        @Size(max = 500) String dataSubjects,
        @Size(max = 255) String volume,
        @Size(max = 255) String format,
        Long storageAssetId,
        @Size(max = 500) String storageLocation,
        @Size(max = 255) String storageCountry,
        @Size(max = 255) String retentionPeriod,
        @Size(max = 255) String deletionMethod,
        Long orgUnitId,
        @Size(max = 2000) String notes
) {
}
