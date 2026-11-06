package com.a9.arbor.grc.modules.asset.web.dto;

import com.a9.arbor.grc.modules.asset.domain.AssetClassification;
import com.a9.arbor.grc.modules.asset.domain.AssetTriState;
import com.a9.arbor.grc.modules.asset.domain.AssetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateAssetRequest(
        @Size(max = 255) String name,
        AssetType assetType,
        @Size(max = 128) String assetSubType,
        @Size(max = 512) String dependencyText,
        @Size(max = 512) String businessProcessService,
        @Size(max = 64) String environment,
        @Size(max = 4000) String description,
        Long ownerUserId,
        @Size(max = 255) String ownerJobTitle,
        @Size(max = 255) String ownerDepartment,
        Long orgUnitId,
        Long custodianUserId,
        @Size(max = 255) String custodianName,
        Long parentId,
        @Size(max = 128) String country,
        @Size(max = 255) String siteBuilding,
        @Size(max = 64) String hostingModel,
        @Size(max = 255) String cloudProviderRegion,
        @Size(max = 64) String dataResidency,
        @Size(max = 255) String dataResidencyOther,
        @Size(max = 255) String hostname,
        @Size(max = 64) String ipAddress,
        @Size(max = 128) String assetTagSerial,
        @Size(max = 255) String platformOsVersion,
        @Min(0) @Max(5) Integer confidentiality,
        @Size(max = 4000) String confidentialityJustification,
        @Min(0) @Max(5) Integer integrityRating,
        @Size(max = 4000) String integrityJustification,
        @Min(0) @Max(5) Integer availability,
        @Size(max = 4000) String availabilityJustification,
        AssetClassification appliedClassification,
        AssetClassification classification,
        BigDecimal agreedRtoHours,
        BigDecimal agreedRpoHours,
        AssetTriState encryptedAtRest,
        AssetTriState encryptedInTransit,
        AssetTriState mfaEnforced,
        AssetTriState backedUp,
        @Size(max = 128) String backupFrequency,
        LocalDate lastRestoreTestDate,
        AssetTriState siemMonitored,
        AssetTriState vulnScanned,
        LocalDate lastPatchDate,
        @Size(max = 128) String accessReviewFrequency,
        @Size(max = 64) String lifecycleStatus,
        LocalDate acquisitionGoLiveDate,
        LocalDate warrantyEndDate,
        LocalDate endOfLifeDate,
        @Size(max = 512) String disposalMethod,
        AssetTriState thirdPartyInvolved,
        @Size(max = 255) String vendorName,
        @Size(max = 255) String contractSlaRef,
        AssetTriState tprmAssessmentDone,
        LocalDate lastReviewedDate,
        @Size(max = 255) String reviewedBy,
        @Size(max = 255) String changeTicketRef,
        @Size(max = 8000) String notes,
        Boolean clearAppliedClassification,
        /** When true: name-only minimum; status DRAFT. When false/null: full register validation → ACTIVE. */
        Boolean asDraft
) {
}
