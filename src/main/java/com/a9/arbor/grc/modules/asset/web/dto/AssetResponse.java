package com.a9.arbor.grc.modules.asset.web.dto;

import com.a9.arbor.grc.modules.asset.domain.Asset;
import com.a9.arbor.grc.modules.asset.domain.AssetCia;
import com.a9.arbor.grc.modules.asset.domain.AssetClassification;
import com.a9.arbor.grc.modules.asset.domain.AssetCriticality;
import com.a9.arbor.grc.modules.asset.domain.AssetHandlingGuidance;
import com.a9.arbor.grc.modules.asset.domain.AssetStatus;
import com.a9.arbor.grc.modules.asset.domain.AssetTriState;
import com.a9.arbor.grc.modules.asset.domain.AssetType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AssetResponse(
        Long id,
        String displayId,
        String name,
        AssetType assetType,
        String assetTypeLabel,
        String assetSubType,
        String dependencyText,
        String businessProcessService,
        String environment,
        AssetStatus status,
        String description,
        Long ownerUserId,
        String ownerJobTitle,
        String ownerDepartment,
        Long orgUnitId,
        Long custodianUserId,
        String custodianName,
        Long parentId,
        String country,
        String siteBuilding,
        String hostingModel,
        String cloudProviderRegion,
        String dataResidency,
        String dataResidencyOther,
        String hostname,
        String ipAddress,
        String assetTagSerial,
        String platformOsVersion,
        Integer confidentiality,
        String confidentialityJustification,
        Integer integrityRating,
        String integrityJustification,
        Integer availability,
        String availabilityJustification,
        BigDecimal ciaScore,
        Integer aggregateScore,
        BigDecimal weightedScore,
        AssetCriticality criticality,
        String criticalityLabel,
        AssetClassification derivedClassification,
        AssetClassification appliedClassification,
        AssetClassification classification,
        Integer classRank,
        String classificationCheck,
        BigDecimal recommendedRtoHours,
        BigDecimal recommendedRpoHours,
        BigDecimal agreedRtoHours,
        BigDecimal agreedRpoHours,
        String recoveryObjectiveCheck,
        Integer reviewCycleMonths,
        String requiredControlBaseline,
        String baselineGaps,
        AssetTriState encryptedAtRest,
        AssetTriState encryptedInTransit,
        AssetTriState mfaEnforced,
        AssetTriState backedUp,
        String backupFrequency,
        LocalDate lastRestoreTestDate,
        AssetTriState siemMonitored,
        AssetTriState vulnScanned,
        LocalDate lastPatchDate,
        String accessReviewFrequency,
        String lifecycleStatus,
        LocalDate acquisitionGoLiveDate,
        LocalDate warrantyEndDate,
        LocalDate endOfLifeDate,
        String eolRiskFlag,
        String disposalMethod,
        AssetTriState thirdPartyInvolved,
        String vendorName,
        String contractSlaRef,
        AssetTriState tprmAssessmentDone,
        LocalDate lastReviewedDate,
        LocalDate nextReviewDue,
        String reviewStatus,
        String reviewedBy,
        String changeTicketRef,
        String notes,
        AssetHandlingGuidance.Guidance handlingGuidance,
        Long createdByUserId,
        Instant disposedAt,
        Long disposedByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static AssetResponse from(Asset asset) {
        boolean rated = AssetCia.allRated(
                asset.getConfidentiality(), asset.getIntegrityRating(), asset.getAvailability());
        AssetClassification derived = rated
                ? AssetCia.classificationFromConfidentiality(asset.getConfidentiality())
                : null;
        AssetClassification applied = asset.getAppliedClassification();
        AssetClassification finalClass = rated ? AssetCia.finalClassification(derived, applied) : null;
        BigDecimal value = !rated ? null
                : asset.getCiaScore() != null
                ? asset.getCiaScore()
                : AssetCia.assetValue(asset.getConfidentiality(), asset.getIntegrityRating(), asset.getAvailability());
        Integer aggregate = !rated ? null
                : asset.getAggregateScore() != null
                ? asset.getAggregateScore()
                : AssetCia.aggregateScore(asset.getConfidentiality(), asset.getIntegrityRating(), asset.getAvailability());
        BigDecimal weighted = !rated ? null
                : asset.getWeightedScore() != null
                ? asset.getWeightedScore()
                : AssetCia.weightedScore(asset.getConfidentiality(), asset.getIntegrityRating(), asset.getAvailability());
        BigDecimal recRto = rated ? AssetCia.recommendedRtoHours(asset.getAvailability()) : null;
        BigDecimal recRpo = rated ? AssetCia.recommendedRpoHours(asset.getAvailability()) : null;
        Integer reviewMonths = rated ? AssetCia.reviewCycleMonths(value) : null;
        LocalDate today = LocalDate.now();
        LocalDate nextReview = reviewMonths != null
                ? AssetCia.nextReviewDue(asset.getLastReviewedDate(), reviewMonths)
                : null;
        List<String> gaps = rated ? AssetCia.baselineGaps(
                finalClass,
                asset.getEncryptedAtRest(),
                asset.getEncryptedInTransit(),
                asset.getMfaEnforced(),
                asset.getBackedUp(),
                asset.getSiemMonitored(),
                asset.getVulnScanned()) : List.of();

        return new AssetResponse(
                asset.getId(),
                asset.getDisplayId(),
                asset.getName(),
                asset.getAssetType(),
                com.a9.arbor.grc.modules.asset.domain.AssetLists.typeLabel(asset.getAssetType()),
                asset.getAssetSubType(),
                asset.getDependencyText(),
                asset.getBusinessProcessService(),
                asset.getEnvironment(),
                asset.getStatus(),
                asset.getDescription(),
                asset.getOwnerUserId(),
                asset.getOwnerJobTitle(),
                asset.getOwnerDepartment(),
                asset.getOrgUnitId(),
                asset.getCustodianUserId(),
                asset.getCustodianName(),
                asset.getParentId(),
                asset.getCountry(),
                asset.getSiteBuilding(),
                asset.getHostingModel(),
                asset.getCloudProviderRegion(),
                asset.getDataResidency(),
                asset.getDataResidencyOther(),
                asset.getHostname(),
                asset.getIpAddress(),
                asset.getAssetTagSerial(),
                asset.getPlatformOsVersion(),
                asset.getConfidentiality(),
                asset.getConfidentialityJustification(),
                asset.getIntegrityRating(),
                asset.getIntegrityJustification(),
                asset.getAvailability(),
                asset.getAvailabilityJustification(),
                value,
                aggregate,
                weighted,
                asset.getCriticality(),
                rated ? AssetCia.criticalityLabel(asset.getCriticality(), value) : null,
                derived,
                applied,
                finalClass,
                rated ? AssetCia.classRank(finalClass) : null,
                rated ? AssetCia.classificationCheck(derived, applied) : null,
                recRto,
                recRpo,
                asset.getAgreedRtoHours(),
                asset.getAgreedRpoHours(),
                rated ? AssetCia.recoveryObjectiveCheck(recRto, recRpo, asset.getAgreedRtoHours(), asset.getAgreedRpoHours()) : null,
                reviewMonths,
                rated ? AssetCia.requiredControlBaselineText(finalClass) : null,
                rated ? AssetCia.baselineGapsText(gaps) : null,
                asset.getEncryptedAtRest(),
                asset.getEncryptedInTransit(),
                asset.getMfaEnforced(),
                asset.getBackedUp(),
                asset.getBackupFrequency(),
                asset.getLastRestoreTestDate(),
                asset.getSiemMonitored(),
                asset.getVulnScanned(),
                asset.getLastPatchDate(),
                asset.getAccessReviewFrequency(),
                asset.getLifecycleStatus(),
                asset.getAcquisitionGoLiveDate(),
                asset.getWarrantyEndDate(),
                asset.getEndOfLifeDate(),
                AssetCia.eolRiskFlag(asset.getEndOfLifeDate(), today),
                asset.getDisposalMethod(),
                asset.getThirdPartyInvolved(),
                asset.getVendorName(),
                asset.getContractSlaRef(),
                asset.getTprmAssessmentDone(),
                asset.getLastReviewedDate(),
                nextReview,
                rated ? AssetCia.reviewStatus(nextReview, today) : null,
                asset.getReviewedBy(),
                asset.getChangeTicketRef(),
                asset.getNotes(),
                rated ? AssetHandlingGuidance.forConfidentiality(asset.getConfidentiality()) : null,
                asset.getCreatedByUserId(),
                asset.getDisposedAt(),
                asset.getDisposedByUserId(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }
}
