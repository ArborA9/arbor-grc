package com.a9.arbor.grc.modules.asset.service;

import com.a9.arbor.grc.modules.asset.domain.Asset;
import com.a9.arbor.grc.modules.asset.domain.AssetCia;
import com.a9.arbor.grc.modules.asset.domain.AssetClassification;
import com.a9.arbor.grc.modules.asset.domain.AssetCriticality;
import com.a9.arbor.grc.modules.asset.domain.AssetHandlingGuidance;
import com.a9.arbor.grc.modules.asset.domain.AssetLists;
import com.a9.arbor.grc.modules.asset.domain.AssetStatus;
import com.a9.arbor.grc.modules.asset.domain.AssetType;
import com.a9.arbor.grc.modules.asset.repo.AssetRepository;
import com.a9.arbor.grc.modules.asset.web.dto.AssetKpiResponse;
import com.a9.arbor.grc.modules.asset.web.dto.AssetMetaResponse;
import com.a9.arbor.grc.modules.asset.web.dto.AssetResponse;
import com.a9.arbor.grc.modules.asset.web.dto.CreateAssetRequest;
import com.a9.arbor.grc.modules.asset.web.dto.UpdateAssetRequest;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserOrgUnitRepository;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UserOrgUnitRepository userOrgUnitRepository;
    private final AuditTrailService auditTrailService;

    public AssetService(AssetRepository assetRepository,
                        UserRepository userRepository,
                        OrgUnitRepository orgUnitRepository,
                        UserOrgUnitRepository userOrgUnitRepository,
                        AuditTrailService auditTrailService) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userOrgUnitRepository = userOrgUnitRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> list(User user) {
        return visibleAssets(user).stream().map(AssetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssetKpiResponse kpis(User user) {
        List<Asset> rows = visibleAssets(user);
        long total = rows.size();
        long active = rows.stream().filter(a -> a.getStatus() == AssetStatus.ACTIVE).count();
        long disposed = rows.stream().filter(a -> a.getStatus() == AssetStatus.DISPOSED).count();
        long tier12 = rows.stream()
                .filter(a -> a.getStatus() != AssetStatus.DISPOSED)
                .filter(a -> a.getCriticality() == AssetCriticality.CRITICAL
                        || a.getCriticality() == AssetCriticality.HIGH)
                .count();
        long secretSensitive = rows.stream()
                .filter(a -> a.getStatus() != AssetStatus.DISPOSED)
                .filter(a -> {
                    AssetClassification c = a.getClassification();
                    return c == AssetClassification.SECRET || c == AssetClassification.SENSITIVE;
                })
                .count();
        return new AssetKpiResponse(total, active, disposed, tier12, secretSensitive);
    }

    @Transactional(readOnly = true)
    public AssetMetaResponse meta() {
        List<AssetMetaResponse.TypeOption> types = new ArrayList<>();
        for (AssetType t : AssetType.values()) {
            types.add(new AssetMetaResponse.TypeOption(t.name(), AssetLists.typeLabel(t)));
        }
        List<AssetMetaResponse.ImpactScaleRow> scale = List.of(
                new AssetMetaResponse.ImpactScaleRow(5,
                        "CATASTROPHIC. Unauthorised disclosure would endanger life, national security or public safety, trigger regulatory sanction, or cause irrecoverable loss of public trust.",
                        "CATASTROPHIC. Undetected corruption would drive wrong operational decisions with risk to life or safety, or render statutory records unreliable.",
                        "CATASTROPHIC. Loss for minutes to an hour halts an emergency or life-safety service.",
                        "Secret"),
                new AssetMetaResponse.ImpactScaleRow(4,
                        "MAJOR. Disclosure would breach law or contract, expose sensitive personal data at scale, or cause significant reputational and financial damage.",
                        "MAJOR. Corruption would cause significant financial loss, regulatory breach, or materially wrong management decisions.",
                        "MAJOR. Loss for a few hours materially disrupts a core service to the public or to another authority.",
                        "Sensitive"),
                new AssetMetaResponse.ImpactScaleRow(3,
                        "MODERATE. Internal-only information whose disclosure would cause embarrassment, operational disadvantage, or limited personal-data exposure.",
                        "MODERATE. Corruption would cause noticeable rework, service errors, or localised financial loss.",
                        "MODERATE. Loss for up to a working day disrupts a business function but not the public-facing service.",
                        "Confidential"),
                new AssetMetaResponse.ImpactScaleRow(2,
                        "MINOR. Information intended for internal use whose disclosure would cause little beyond inconvenience.",
                        "MINOR. Errors would be spotted and corrected easily, with minimal consequence.",
                        "MINOR. Loss for several days is tolerable using manual workarounds.",
                        "Confidential"),
                new AssetMetaResponse.ImpactScaleRow(1,
                        "NEGLIGIBLE. Already public, or approved for public release.",
                        "NEGLIGIBLE. Accuracy is not material to any decision or obligation.",
                        "NEGLIGIBLE. Loss for a week or more has no material effect.",
                        "Open"));
        List<AssetMetaResponse.HandlingRow> handling = List.of(1, 2, 3, 4, 5).stream()
                .map(AssetHandlingGuidance::forConfidentiality)
                .map(g -> new AssetMetaResponse.HandlingRow(
                        g.confidentiality(), g.sheetLabel(), g.labelling(), g.storage(),
                        g.transfer(), g.access(), g.disposal()))
                .toList();
        return new AssetMetaResponse(
                AssetLists.CLASSIFICATIONS,
                types,
                AssetLists.ASSET_SUB_TYPES,
                AssetLists.HOSTING_MODELS,
                AssetLists.DATA_RESIDENCIES,
                AssetLists.ENVIRONMENTS,
                AssetLists.LIFECYCLE_STATUSES,
                AssetLists.TRI_STATES,
                scale,
                handling);
    }

    @Transactional(readOnly = true)
    public AssetResponse get(User user, long id) {
        Asset asset = requireVisible(user, id);
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse create(User user, CreateAssetRequest req) {
        boolean draft = Boolean.TRUE.equals(req.asDraft());
        validateOptionalRefs(req.ownerUserId(), req.orgUnitId(), req.custodianUserId());
        validateParent(req.parentId(), null);
        if (draft) {
            requireDraftName(req.name());
            if (req.assetType() == AssetType.OTHER) {
                validateAssetType(req.assetType(), req.assetSubType());
            }
        } else {
            validateAssetType(req.assetType(), req.assetSubType());
            requireRegisterFields(req.description(), req.environment(), req.ownerUserId(),
                    req.ownerDepartment(), req.orgUnitId(), req.custodianUserId(), req.custodianName());
            requireJustifications(req.confidentialityJustification(), req.integrityJustification(),
                    req.availabilityJustification());
            requireOtherResidencyIfNeeded(req.dataResidency(), req.dataResidencyOther());
            requireCiaRatings(
                    normalizeCia(req.confidentiality()),
                    normalizeCia(req.integrityRating()),
                    normalizeCia(req.availability()));
        }
        String name = req.name().trim();
        if (assetRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An asset named \"" + name + "\" already exists");
        }

        Asset asset = new Asset();
        applyCreateFields(asset, req);
        Integer c = normalizeCia(req.confidentiality());
        Integer i = normalizeCia(req.integrityRating());
        Integer a = normalizeCia(req.availability());
        applyCiaOrClear(asset, c, i, a, AssetCia.isRated(c)
                ? resolveApplied(c, req.appliedClassification(), req.classification())
                : null);
        asset.setStatus(draft ? AssetStatus.DRAFT : AssetStatus.ACTIVE);
        if (draft) {
            asset.setLifecycleStatus("Draft");
        }
        asset.setCreatedByUserId(user.getId());
        assetRepository.save(asset);
        if (asset.getDisplayId() == null || asset.getDisplayId().isBlank()) {
            asset.setDisplayId(formatDisplayId(asset.getId()));
            assetRepository.save(asset);
        }

        audit(user, AuditActionType.CREATED, String.valueOf(asset.getId()), asset.getName(), null, null);
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse update(User user, long id, UpdateAssetRequest req) {
        Asset asset = require(id);
        assertEditable(asset);
        boolean draft = Boolean.TRUE.equals(req.asDraft());
        validateOptionalRefs(req.ownerUserId(), req.orgUnitId(), req.custodianUserId());
        if (req.parentId() != null) {
            validateParent(req.parentId(), id);
        }
        if (draft) {
            String name = req.name() != null ? req.name() : asset.getName();
            requireDraftName(name);
        } else {
            requireRegisterFieldsOnUpdate(asset, req);
            AssetType type = req.assetType() != null ? req.assetType() : asset.getAssetType();
            String sub = req.assetSubType() != null ? req.assetSubType() : asset.getAssetSubType();
            validateAssetType(type, sub);
        }

        applyUpdateFields(asset, req);
        if (draft) {
            Integer c = normalizeCia(req.confidentiality());
            Integer i = normalizeCia(req.integrityRating());
            Integer a = normalizeCia(req.availability());
            AssetClassification applied = AssetCia.isRated(c)
                    ? resolveApplied(c, req.appliedClassification(), req.classification())
                    : null;
            applyCiaOrClear(asset, c, i, a, applied);
        } else if (req.confidentiality() != null || req.integrityRating() != null || req.availability() != null
                || req.appliedClassification() != null || req.classification() != null
                || Boolean.TRUE.equals(req.clearAppliedClassification())) {
            Integer c = req.confidentiality() != null
                    ? normalizeCia(req.confidentiality()) : asset.getConfidentiality();
            Integer i = req.integrityRating() != null
                    ? normalizeCia(req.integrityRating()) : asset.getIntegrityRating();
            Integer a = req.availability() != null
                    ? normalizeCia(req.availability()) : asset.getAvailability();
            requireCiaRatings(c, i, a);
            AssetClassification applied = asset.getAppliedClassification();
            if (Boolean.TRUE.equals(req.clearAppliedClassification())) {
                applied = null;
            } else if (req.appliedClassification() != null || req.classification() != null) {
                applied = resolveApplied(c, req.appliedClassification(), req.classification());
            }
            applyCia(asset, c, i, a, applied);
        }
        if (draft) {
            asset.setStatus(AssetStatus.DRAFT);
            asset.setLifecycleStatus("Draft");
        } else {
            asset.setStatus(AssetStatus.ACTIVE);
            if (asset.getLifecycleStatus() == null || "Draft".equalsIgnoreCase(asset.getLifecycleStatus())
                    || "Disposed".equalsIgnoreCase(asset.getLifecycleStatus())) {
                asset.setLifecycleStatus("Active");
            }
        }
        assetRepository.save(asset);

        audit(user, AuditActionType.UPDATED, String.valueOf(asset.getId()), asset.getName(), null, null);
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse dispose(User user, long id) {
        Asset asset = require(id);
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            return AssetResponse.from(asset);
        }
        asset.setStatus(AssetStatus.DISPOSED);
        asset.setLifecycleStatus("Disposed");
        asset.setDisposedAt(Instant.now());
        asset.setDisposedByUserId(user.getId());
        assetRepository.save(asset);

        audit(user, AuditActionType.UPDATED, String.valueOf(asset.getId()), asset.getName(), "status", "DISPOSED");
        return AssetResponse.from(asset);
    }

    private List<Asset> visibleAssets(User user) {
        if (RoleAccess.isContributorOnly(user)) {
            List<Long> unitIds = userOrgUnitRepository.findByUserId(user.getId()).stream()
                    .map(u -> u.getOrgUnitId())
                    .toList();
            if (unitIds.isEmpty()) {
                return List.of();
            }
            return assetRepository.findByOrgUnitIdInOrderByUpdatedAtDesc(unitIds);
        }
        return assetRepository.findAllByOrderByUpdatedAtDesc();
    }

    private void applyCreateFields(Asset asset, CreateAssetRequest req) {
        asset.setName(req.name().trim());
        asset.setAssetType(req.assetType());
        asset.setAssetSubType(blankToNull(req.assetSubType()));
        asset.setDependencyText(blankToNull(req.dependencyText()));
        asset.setBusinessProcessService(blankToNull(req.businessProcessService()));
        asset.setEnvironment(blankToNull(req.environment()));
        asset.setDescription(blankToNull(req.description()));
        asset.setOwnerUserId(req.ownerUserId());
        asset.setOwnerJobTitle(blankToNull(req.ownerJobTitle()));
        asset.setOwnerDepartment(resolveDepartment(req.ownerDepartment(), req.orgUnitId()));
        asset.setOrgUnitId(req.orgUnitId());
        asset.setCustodianUserId(req.custodianUserId());
        asset.setCustodianName(blankToNull(req.custodianName()));
        asset.setParentId(req.parentId());
        asset.setCountry(blankToNull(req.country()));
        asset.setSiteBuilding(blankToNull(req.siteBuilding()));
        asset.setHostingModel(blankToNull(req.hostingModel()));
        asset.setCloudProviderRegion(blankToNull(req.cloudProviderRegion()));
        asset.setDataResidency(blankToNull(req.dataResidency()));
        applyDataResidencyOther(asset, req.dataResidencyOther());
        asset.setHostname(blankToNull(req.hostname()));
        asset.setIpAddress(blankToNull(req.ipAddress()));
        asset.setAssetTagSerial(blankToNull(req.assetTagSerial()));
        asset.setPlatformOsVersion(blankToNull(req.platformOsVersion()));
        asset.setConfidentialityJustification(blankToNull(req.confidentialityJustification()));
        asset.setIntegrityJustification(blankToNull(req.integrityJustification()));
        asset.setAvailabilityJustification(blankToNull(req.availabilityJustification()));
        asset.setAgreedRtoHours(req.agreedRtoHours());
        asset.setAgreedRpoHours(req.agreedRpoHours());
        asset.setEncryptedAtRest(req.encryptedAtRest());
        asset.setEncryptedInTransit(req.encryptedInTransit());
        asset.setMfaEnforced(req.mfaEnforced());
        asset.setBackedUp(req.backedUp());
        asset.setBackupFrequency(blankToNull(req.backupFrequency()));
        asset.setLastRestoreTestDate(req.lastRestoreTestDate());
        asset.setSiemMonitored(req.siemMonitored());
        asset.setVulnScanned(req.vulnScanned());
        asset.setLastPatchDate(req.lastPatchDate());
        asset.setAccessReviewFrequency(blankToNull(req.accessReviewFrequency()));
        asset.setLifecycleStatus(blankToNull(req.lifecycleStatus()) != null
                ? req.lifecycleStatus().trim() : "Active");
        asset.setAcquisitionGoLiveDate(req.acquisitionGoLiveDate());
        asset.setWarrantyEndDate(req.warrantyEndDate());
        asset.setEndOfLifeDate(req.endOfLifeDate());
        asset.setDisposalMethod(blankToNull(req.disposalMethod()));
        asset.setThirdPartyInvolved(req.thirdPartyInvolved());
        asset.setVendorName(blankToNull(req.vendorName()));
        asset.setContractSlaRef(blankToNull(req.contractSlaRef()));
        asset.setTprmAssessmentDone(req.tprmAssessmentDone());
        asset.setLastReviewedDate(req.lastReviewedDate());
        asset.setReviewedBy(blankToNull(req.reviewedBy()));
        asset.setChangeTicketRef(blankToNull(req.changeTicketRef()));
        asset.setNotes(blankToNull(req.notes()));
    }

    private void applyUpdateFields(Asset asset, UpdateAssetRequest req) {
        if (req.name() != null && !req.name().isBlank()) {
            String name = req.name().trim();
            if (assetRepository.existsByNameIgnoreCaseAndIdNot(name, asset.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An asset named \"" + name + "\" already exists");
            }
            asset.setName(name);
        }
        if (req.assetType() != null) {
            asset.setAssetType(req.assetType());
        }
        if (req.assetSubType() != null || req.assetType() == AssetType.OTHER) {
            String sub = req.assetSubType() != null ? req.assetSubType() : asset.getAssetSubType();
            validateAssetType(asset.getAssetType(), sub);
            asset.setAssetSubType(blankToNull(sub));
        }
        if (req.dependencyText() != null) {
            asset.setDependencyText(blankToNull(req.dependencyText()));
        }
        if (req.businessProcessService() != null) {
            asset.setBusinessProcessService(blankToNull(req.businessProcessService()));
        }
        if (req.environment() != null) {
            asset.setEnvironment(blankToNull(req.environment()));
        }
        if (req.description() != null) {
            asset.setDescription(blankToNull(req.description()));
        }
        if (req.ownerUserId() != null) {
            asset.setOwnerUserId(req.ownerUserId());
        }
        if (req.ownerJobTitle() != null) {
            asset.setOwnerJobTitle(blankToNull(req.ownerJobTitle()));
        }
        if (req.orgUnitId() != null) {
            asset.setOrgUnitId(req.orgUnitId());
        }
        if (req.ownerDepartment() != null) {
            asset.setOwnerDepartment(blankToNull(req.ownerDepartment()));
        } else if (req.orgUnitId() != null && (asset.getOwnerDepartment() == null || asset.getOwnerDepartment().isBlank())) {
            asset.setOwnerDepartment(resolveDepartment(null, req.orgUnitId()));
        }
        if (req.custodianUserId() != null) {
            asset.setCustodianUserId(req.custodianUserId());
        }
        if (req.custodianName() != null) {
            asset.setCustodianName(blankToNull(req.custodianName()));
        }
        if (req.parentId() != null) {
            asset.setParentId(req.parentId());
        }
        if (req.country() != null) {
            asset.setCountry(blankToNull(req.country()));
        }
        if (req.siteBuilding() != null) {
            asset.setSiteBuilding(blankToNull(req.siteBuilding()));
        }
        if (req.hostingModel() != null) {
            asset.setHostingModel(blankToNull(req.hostingModel()));
        }
        if (req.cloudProviderRegion() != null) {
            asset.setCloudProviderRegion(blankToNull(req.cloudProviderRegion()));
        }
        if (req.dataResidency() != null) {
            asset.setDataResidency(blankToNull(req.dataResidency()));
        }
        if (req.dataResidency() != null || req.dataResidencyOther() != null) {
            String other = req.dataResidencyOther() != null
                    ? req.dataResidencyOther()
                    : asset.getDataResidencyOther();
            applyDataResidencyOther(asset, other);
        }
        if (req.hostname() != null) {
            asset.setHostname(blankToNull(req.hostname()));
        }
        if (req.ipAddress() != null) {
            asset.setIpAddress(blankToNull(req.ipAddress()));
        }
        if (req.assetTagSerial() != null) {
            asset.setAssetTagSerial(blankToNull(req.assetTagSerial()));
        }
        if (req.platformOsVersion() != null) {
            asset.setPlatformOsVersion(blankToNull(req.platformOsVersion()));
        }
        if (req.confidentialityJustification() != null) {
            asset.setConfidentialityJustification(blankToNull(req.confidentialityJustification()));
        }
        if (req.integrityJustification() != null) {
            asset.setIntegrityJustification(blankToNull(req.integrityJustification()));
        }
        if (req.availabilityJustification() != null) {
            asset.setAvailabilityJustification(blankToNull(req.availabilityJustification()));
        }
        if (req.agreedRtoHours() != null) {
            asset.setAgreedRtoHours(req.agreedRtoHours());
        }
        if (req.agreedRpoHours() != null) {
            asset.setAgreedRpoHours(req.agreedRpoHours());
        }
        if (req.encryptedAtRest() != null) {
            asset.setEncryptedAtRest(req.encryptedAtRest());
        }
        if (req.encryptedInTransit() != null) {
            asset.setEncryptedInTransit(req.encryptedInTransit());
        }
        if (req.mfaEnforced() != null) {
            asset.setMfaEnforced(req.mfaEnforced());
        }
        if (req.backedUp() != null) {
            asset.setBackedUp(req.backedUp());
        }
        if (req.backupFrequency() != null) {
            asset.setBackupFrequency(blankToNull(req.backupFrequency()));
        }
        if (req.lastRestoreTestDate() != null) {
            asset.setLastRestoreTestDate(req.lastRestoreTestDate());
        }
        if (req.siemMonitored() != null) {
            asset.setSiemMonitored(req.siemMonitored());
        }
        if (req.vulnScanned() != null) {
            asset.setVulnScanned(req.vulnScanned());
        }
        if (req.lastPatchDate() != null) {
            asset.setLastPatchDate(req.lastPatchDate());
        }
        if (req.accessReviewFrequency() != null) {
            asset.setAccessReviewFrequency(blankToNull(req.accessReviewFrequency()));
        }
        if (req.lifecycleStatus() != null) {
            asset.setLifecycleStatus(blankToNull(req.lifecycleStatus()));
        }
        if (req.acquisitionGoLiveDate() != null) {
            asset.setAcquisitionGoLiveDate(req.acquisitionGoLiveDate());
        }
        if (req.warrantyEndDate() != null) {
            asset.setWarrantyEndDate(req.warrantyEndDate());
        }
        if (req.endOfLifeDate() != null) {
            asset.setEndOfLifeDate(req.endOfLifeDate());
        }
        if (req.disposalMethod() != null) {
            asset.setDisposalMethod(blankToNull(req.disposalMethod()));
        }
        if (req.thirdPartyInvolved() != null) {
            asset.setThirdPartyInvolved(req.thirdPartyInvolved());
        }
        if (req.vendorName() != null) {
            asset.setVendorName(blankToNull(req.vendorName()));
        }
        if (req.contractSlaRef() != null) {
            asset.setContractSlaRef(blankToNull(req.contractSlaRef()));
        }
        if (req.tprmAssessmentDone() != null) {
            asset.setTprmAssessmentDone(req.tprmAssessmentDone());
        }
        if (req.lastReviewedDate() != null) {
            asset.setLastReviewedDate(req.lastReviewedDate());
        }
        if (req.reviewedBy() != null) {
            asset.setReviewedBy(blankToNull(req.reviewedBy()));
        }
        if (req.changeTicketRef() != null) {
            asset.setChangeTicketRef(blankToNull(req.changeTicketRef()));
        }
        if (req.notes() != null) {
            asset.setNotes(blankToNull(req.notes()));
        }
    }

    private Asset require(long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    }

    private Asset requireVisible(User user, long id) {
        Asset asset = require(id);
        if (RoleAccess.isContributorOnly(user)) {
            List<Long> unitIds = userOrgUnitRepository.findByUserId(user.getId()).stream()
                    .map(u -> u.getOrgUnitId())
                    .toList();
            if (asset.getOrgUnitId() == null || !unitIds.contains(asset.getOrgUnitId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Asset not in your org units");
            }
        }
        return asset;
    }

    private void validateOptionalRefs(Long ownerUserId, Long orgUnitId, Long custodianUserId) {
        if (ownerUserId != null && userRepository.findById(ownerUserId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found");
        }
        if (custodianUserId != null && userRepository.findById(custodianUserId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custodian user not found");
        }
        if (orgUnitId != null && orgUnitRepository.findById(orgUnitId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org unit not found");
        }
    }

    private String resolveDepartment(String explicit, Long orgUnitId) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        if (orgUnitId == null) {
            return null;
        }
        return orgUnitRepository.findById(orgUnitId).map(OrgUnit::getName).orElse(null);
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset cannot be its own parent");
        }
        Asset parent = assetRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent asset not found"));
        if (parent.getStatus() == AssetStatus.DISPOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent asset is disposed");
        }
        if (parent.getParentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent asset already has a parent (max 2 levels)");
        }
    }

    private static void validateAssetType(AssetType type, String assetSubType) {
        if (type == AssetType.OTHER && (assetSubType == null || assetSubType.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Asset subtype is required when type is OTHER");
        }
    }

    /** Matches Asset Register Save rules (UI + Excel start guidance). */
    private static void requireDraftName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Asset name is required to save a draft");
        }
    }

    private static void requireRegisterFields(String description, String environment, Long ownerUserId,
                                              String ownerDepartment, Long orgUnitId,
                                              Long custodianUserId, String custodianName) {
        if (blankToNull(description) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset description is required");
        }
        if (blankToNull(environment) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Environment is required");
        }
        if (ownerUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset owner is required");
        }
        if (blankToNull(ownerDepartment) == null && orgUnitId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner department is required");
        }
        if (custodianUserId == null && blankToNull(custodianName) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset custodian is required");
        }
    }

    /**
     * Full Save from UI sends register fields. Reject blanking required values;
     * omit (null) keeps the existing value on partial API updates.
     */
    private static void requireRegisterFieldsOnUpdate(Asset asset, UpdateAssetRequest req) {
        String description = req.description() != null ? req.description() : asset.getDescription();
        String environment = req.environment() != null ? req.environment() : asset.getEnvironment();
        Long ownerUserId = req.ownerUserId() != null ? req.ownerUserId() : asset.getOwnerUserId();
        String ownerDepartment = req.ownerDepartment() != null
                ? req.ownerDepartment() : asset.getOwnerDepartment();
        Long orgUnitId = req.orgUnitId() != null ? req.orgUnitId() : asset.getOrgUnitId();
        Long custodianUserId = req.custodianUserId() != null
                ? req.custodianUserId() : asset.getCustodianUserId();
        String custodianName = req.custodianName() != null
                ? req.custodianName() : asset.getCustodianName();
        requireRegisterFields(description, environment, ownerUserId, ownerDepartment, orgUnitId,
                custodianUserId, custodianName);
        String residency = req.dataResidency() != null ? req.dataResidency() : asset.getDataResidency();
        String residencyOther = req.dataResidencyOther() != null
                ? req.dataResidencyOther() : asset.getDataResidencyOther();
        requireOtherResidencyIfNeeded(residency, residencyOther);
        Integer confidentiality = req.confidentiality() != null
                ? normalizeCia(req.confidentiality()) : asset.getConfidentiality();
        Integer integrity = req.integrityRating() != null
                ? normalizeCia(req.integrityRating()) : asset.getIntegrityRating();
        Integer availability = req.availability() != null
                ? normalizeCia(req.availability()) : asset.getAvailability();
        requireCiaRatings(confidentiality, integrity, availability);
        if (req.confidentialityJustification() != null || req.integrityJustification() != null
                || req.availabilityJustification() != null) {
            requireJustifications(
                    req.confidentialityJustification() != null
                            ? req.confidentialityJustification() : asset.getConfidentialityJustification(),
                    req.integrityJustification() != null
                            ? req.integrityJustification() : asset.getIntegrityJustification(),
                    req.availabilityJustification() != null
                            ? req.availabilityJustification() : asset.getAvailabilityJustification());
        }
    }

    private static void requireJustifications(String cJust, String iJust, String aJust) {
        if (blankToNull(cJust) == null || blankToNull(iJust) == null || blankToNull(aJust) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CIA justifications are required (Confidentiality, Integrity, Availability)");
        }
    }

    private static void requireCiaRatings(Integer confidentiality, Integer integrity, Integer availability) {
        if (!AssetCia.allRated(confidentiality, integrity, availability)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CIA ratings are required (Confidentiality, Integrity, Availability)");
        }
    }

    /** Treat 0 from older clients as unset. Rated values stay 1-5. */
    private static Integer normalizeCia(Integer value) {
        if (value == null || value == 0) {
            return null;
        }
        return value;
    }

    /** Treat legacy {@code classification} / applied as override only when it differs from derived. */
    private static AssetClassification resolveApplied(int confidentiality,
                                                      AssetClassification applied,
                                                      AssetClassification legacyClassification) {
        AssetClassification candidate = applied != null ? applied : legacyClassification;
        if (candidate == null) {
            return null;
        }
        AssetClassification derived = AssetCia.classificationFromConfidentiality(confidentiality);
        return candidate == derived ? null : candidate;
    }

    private static void applyCiaOrClear(Asset asset, Integer c, Integer i, Integer a,
                                        AssetClassification applied) {
        if (AssetCia.allRated(c, i, a)) {
            applyCia(asset, c, i, a, applied);
            return;
        }
        asset.setConfidentiality(c);
        asset.setIntegrityRating(i);
        asset.setAvailability(a);
        asset.setCiaScore(null);
        asset.setAggregateScore(null);
        asset.setWeightedScore(null);
        asset.setCriticality(null);
        asset.setAppliedClassification(null);
        asset.setClassification(null);
    }

    private static void applyCia(Asset asset, int c, int i, int a, AssetClassification applied) {
        asset.setConfidentiality(c);
        asset.setIntegrityRating(i);
        asset.setAvailability(a);
        var score = AssetCia.assetValue(c, i, a);
        asset.setCiaScore(score);
        asset.setAggregateScore(AssetCia.aggregateScore(c, i, a));
        asset.setWeightedScore(AssetCia.weightedScore(c, i, a));
        asset.setCriticality(AssetCia.criticalityFromAssetValue(score));
        AssetClassification derived = AssetCia.classificationFromConfidentiality(c);
        asset.setAppliedClassification(applied);
        asset.setClassification(AssetCia.finalClassification(derived, applied));
    }

    private static String formatDisplayId(long id) {
        return String.format("AST-%04d", id);
    }

    private static void assertEditable(Asset asset) {
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Disposed assets cannot be edited");
        }
    }

    private void audit(User user, AuditActionType action, String recordId, String recordName,
                       String field, String newVal) {
        auditTrailService.record(
                action, "ASSET", user.getId(), primaryRole(user),
                "ASSET", recordId, recordName, field, null, newVal, null, null);
    }

    private static String primaryRole(User user) {
        return user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
    }

    private static boolean isOtherResidency(String residency) {
        return residency != null && residency.trim().equalsIgnoreCase("Other");
    }

    private static void applyDataResidencyOther(Asset asset, String other) {
        if (isOtherResidency(asset.getDataResidency())) {
            asset.setDataResidencyOther(blankToNull(other));
        } else {
            asset.setDataResidencyOther(null);
        }
    }

    private static void requireOtherResidencyIfNeeded(String residency, String other) {
        if (isOtherResidency(residency) && blankToNull(other) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter the data residency location when Other is selected");
        }
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
