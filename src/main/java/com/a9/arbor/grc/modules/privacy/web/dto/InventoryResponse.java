package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.InventoryStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDataInventory;

import java.time.Instant;

public record InventoryResponse(
        Long id,
        String displayId,
        String dataType,
        String dataSubjects,
        String volume,
        String format,
        Long storageAssetId,
        String storageLocation,
        String storageCountry,
        String retentionPeriod,
        String deletionMethod,
        Long orgUnitId,
        InventoryStatus status,
        String notes,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static InventoryResponse from(PrivacyDataInventory e) {
        return new InventoryResponse(
                e.getId(),
                e.getDisplayId(),
                e.getDataType(),
                e.getDataSubjects(),
                e.getVolume(),
                e.getFormat(),
                e.getStorageAssetId(),
                e.getStorageLocation(),
                e.getStorageCountry(),
                e.getRetentionPeriod(),
                e.getDeletionMethod(),
                e.getOrgUnitId(),
                e.getStatus(),
                e.getNotes(),
                e.getCreatedByUserId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
