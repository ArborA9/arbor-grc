package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.asset.repo.AssetRepository;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.privacy.domain.InventoryStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDataInventory;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyDataInventoryRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateInventoryRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.InventoryResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateInventoryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PrivacyInventoryService {

    private final PrivacyDataInventoryRepository inventoryRepository;
    private final AssetRepository assetRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AuditTrailService auditTrailService;
    private final UserService userService;

    public PrivacyInventoryService(PrivacyDataInventoryRepository inventoryRepository,
                                   AssetRepository assetRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   AuditTrailService auditTrailService,
                                   UserService userService) {
        this.inventoryRepository = inventoryRepository;
        this.assetRepository = assetRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.auditTrailService = auditTrailService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> list(User viewer) {
        return inventoryRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(row -> canView(viewer, row))
                .map(InventoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(User viewer, long id) {
        PrivacyDataInventory row = require(id);
        if (!canView(viewer, row)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this inventory record");
        }
        return InventoryResponse.from(row);
    }

    @Transactional
    public InventoryResponse create(User user, CreateInventoryRequest req) {
        validateRefs(req.storageAssetId(), req.orgUnitId());

        PrivacyDataInventory row = new PrivacyDataInventory();
        row.setDisplayId(nextDisplayId());
        row.setDataType(req.dataType().trim());
        row.setDataSubjects(blankToNull(req.dataSubjects()));
        row.setVolume(blankToNull(req.volume()));
        row.setFormat(blankToNull(req.format()));
        row.setStorageAssetId(req.storageAssetId());
        row.setStorageLocation(blankToNull(req.storageLocation()));
        row.setStorageCountry(blankToNull(req.storageCountry()));
        row.setRetentionPeriod(blankToNull(req.retentionPeriod()));
        row.setDeletionMethod(blankToNull(req.deletionMethod()));
        row.setOrgUnitId(req.orgUnitId());
        row.setStatus(InventoryStatus.ACTIVE);
        row.setNotes(blankToNull(req.notes()));
        row.setCreatedByUserId(user.getId());
        inventoryRepository.save(row);

        audit(user, AuditActionType.CREATED, row, null, null);
        return InventoryResponse.from(row);
    }

    @Transactional
    public InventoryResponse update(User user, long id, UpdateInventoryRequest req) {
        PrivacyDataInventory row = require(id);
        assertActive(row);
        validateRefs(req.storageAssetId(), req.orgUnitId());

        if (req.dataType() != null && !req.dataType().isBlank()) {
            row.setDataType(req.dataType().trim());
        }
        if (req.dataSubjects() != null) {
            row.setDataSubjects(blankToNull(req.dataSubjects()));
        }
        if (req.volume() != null) {
            row.setVolume(blankToNull(req.volume()));
        }
        if (req.format() != null) {
            row.setFormat(blankToNull(req.format()));
        }
        if (req.storageAssetId() != null) {
            row.setStorageAssetId(req.storageAssetId());
        }
        if (req.storageLocation() != null) {
            row.setStorageLocation(blankToNull(req.storageLocation()));
        }
        if (req.storageCountry() != null) {
            row.setStorageCountry(blankToNull(req.storageCountry()));
        }
        if (req.retentionPeriod() != null) {
            row.setRetentionPeriod(blankToNull(req.retentionPeriod()));
        }
        if (req.deletionMethod() != null) {
            row.setDeletionMethod(blankToNull(req.deletionMethod()));
        }
        if (req.orgUnitId() != null) {
            row.setOrgUnitId(req.orgUnitId());
        }
        if (req.notes() != null) {
            row.setNotes(blankToNull(req.notes()));
        }
        inventoryRepository.save(row);

        audit(user, AuditActionType.UPDATED, row, null, null);
        return InventoryResponse.from(row);
    }

    @Transactional
    public InventoryResponse archive(User user, long id) {
        PrivacyDataInventory row = require(id);
        if (row.getStatus() == InventoryStatus.ARCHIVED) {
            return InventoryResponse.from(row);
        }
        String previous = row.getStatus().name();
        row.setStatus(InventoryStatus.ARCHIVED);
        inventoryRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, InventoryStatus.ARCHIVED.name());
        return InventoryResponse.from(row);
    }

    private PrivacyDataInventory require(long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory record not found"));
    }

    private void assertActive(PrivacyDataInventory row) {
        if (row.getStatus() == InventoryStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived inventory cannot be edited");
        }
    }

    private void validateRefs(Long storageAssetId, Long orgUnitId) {
        if (storageAssetId != null && assetRepository.findById(storageAssetId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage asset not found");
        }
        if (orgUnitId != null && orgUnitRepository.findById(orgUnitId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org unit not found");
        }
    }

    private String nextDisplayId() {
        int year = Year.now().getValue();
        String prefix = "INV-" + year + "-";
        long count = inventoryRepository.countByDisplayIdStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void audit(User user, AuditActionType action, PrivacyDataInventory row,
                       String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "PRIVACY_INVENTORY", user.getId(), primaryRole(user),
                "PRIVACY_INVENTORY", String.valueOf(row.getId()), row.getDisplayId(),
                field, oldVal, newVal, null, null);
    }

    private void audit(User user, AuditActionType action, PrivacyDataInventory row,
                       String field, String newVal) {
        audit(user, action, row, field, null, newVal);
    }

    private static String primaryRole(User user) {
        return user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private boolean canView(User viewer, PrivacyDataInventory row) {
        if (viewer == null || !RoleAccess.isContributorOnly(viewer)) {
            return true;
        }
        if (row.getOrgUnitId() == null) {
            return false;
        }
        Set<Long> allowed = new HashSet<>(userService.orgUnitIdsFor(viewer.getId()));
        return allowed.contains(row.getOrgUnitId());
    }
}
