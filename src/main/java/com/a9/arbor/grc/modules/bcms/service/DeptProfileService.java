package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.bcms.domain.BcmsDeptProfile;
import com.a9.arbor.grc.modules.bcms.domain.BcmsDeptProfileAsset;
import com.a9.arbor.grc.modules.bcms.repo.BcmsDeptProfileAssetRepository;
import com.a9.arbor.grc.modules.bcms.repo.BcmsDeptProfileRepository;
import com.a9.arbor.grc.modules.bcms.web.dto.BcmsDeptProfileResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.UpsertDeptProfileRequest;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DeptProfileService {

    private final BcmsDeptProfileRepository profileRepository;
    private final BcmsDeptProfileAssetRepository profileAssetRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AuditTrailService auditTrailService;

    public DeptProfileService(BcmsDeptProfileRepository profileRepository,
                              BcmsDeptProfileAssetRepository profileAssetRepository,
                              OrgUnitRepository orgUnitRepository,
                              AuditTrailService auditTrailService) {
        this.profileRepository = profileRepository;
        this.profileAssetRepository = profileAssetRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<BcmsDeptProfileResponse> list() {
        return profileRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(p -> BcmsDeptProfileResponse.from(p, assetIds(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BcmsDeptProfileResponse get(long id) {
        BcmsDeptProfile p = require(id);
        return BcmsDeptProfileResponse.from(p, assetIds(p.getId()));
    }

    @Transactional(readOnly = true)
    public BcmsDeptProfileResponse getByOrgUnit(long orgUnitId) {
        BcmsDeptProfile p = profileRepository.findByOrgUnitId(orgUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No department profile for this unit"));
        return BcmsDeptProfileResponse.from(p, assetIds(p.getId()));
    }

    @Transactional
    public BcmsDeptProfileResponse upsert(User admin, UpsertDeptProfileRequest req) {
        if (!orgUnitRepository.existsById(req.orgUnitId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organisation unit not found");
        }
        BcmsDeptProfile p = profileRepository.findByOrgUnitId(req.orgUnitId()).orElseGet(BcmsDeptProfile::new);
        boolean creating = p.getId() == null;
        p.setOrgUnitId(req.orgUnitId());
        p.setTotalStaff(req.totalStaff());
        p.setCriticalStaffRoles(blankToNull(req.criticalStaffRoles()));
        p.setPrimaryFunctions(blankToNull(req.primaryFunctions()));
        p.setInternalDependencies(blankToNull(req.internalDependencies()));
        p.setExternalDependencies(blankToNull(req.externalDependencies()));
        p.setPhysicalLocation(blankToNull(req.physicalLocation()));
        p.setAlternateWorkLocation(blankToNull(req.alternateWorkLocation()));
        profileRepository.save(p);

        profileAssetRepository.deleteByProfileId(p.getId());
        if (req.assetIds() != null) {
            for (Long assetId : req.assetIds()) {
                BcmsDeptProfileAsset link = new BcmsDeptProfileAsset();
                link.setProfileId(p.getId());
                link.setAssetId(assetId);
                profileAssetRepository.save(link);
            }
        }

        audit(admin, creating ? AuditActionType.CREATED : AuditActionType.UPDATED, p);
        return BcmsDeptProfileResponse.from(p, assetIds(p.getId()));
    }

    private List<Long> assetIds(long profileId) {
        return profileAssetRepository.findByProfileId(profileId).stream()
                .map(BcmsDeptProfileAsset::getAssetId)
                .toList();
    }

    private BcmsDeptProfile require(long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department profile not found"));
    }

    private void audit(User user, AuditActionType action, BcmsDeptProfile p) {
        auditTrailService.record(action, "BCMS", user.getId(), rolesSnap(user),
                "BCMS_DEPT_PROFILE", String.valueOf(p.getId()), "Org unit " + p.getOrgUnitId(),
                null, null, null, null, null);
    }

    private static String rolesSnap(User user) {
        return user.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
