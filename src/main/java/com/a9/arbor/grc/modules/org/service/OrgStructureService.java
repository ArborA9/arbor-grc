package com.a9.arbor.grc.modules.org.service;

import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.org.domain.OrgLevelConfig;
import com.a9.arbor.grc.modules.org.domain.OrgProfile;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgLevelConfigRepository;
import com.a9.arbor.grc.modules.org.repo.OrgProfileRepository;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.org.web.dto.CreateOrgUnitRequest;
import com.a9.arbor.grc.modules.org.web.dto.OrgLevelResponse;
import com.a9.arbor.grc.modules.org.web.dto.OrgProfileResponse;
import com.a9.arbor.grc.modules.org.web.dto.OrgUnitResponse;
import com.a9.arbor.grc.modules.org.web.dto.SessionPolicyResponse;
import com.a9.arbor.grc.modules.org.web.dto.UpdateOrgLevelsRequest;
import com.a9.arbor.grc.modules.org.web.dto.UpdateOrgProfileRequest;
import com.a9.arbor.grc.modules.org.web.dto.UpdateOrgUnitRequest;
import com.a9.arbor.grc.modules.risk.service.RiskMatrixService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrgStructureService {

    private final OrgProfileRepository profileRepository;
    private final OrgLevelConfigRepository levelRepository;
    private final OrgUnitRepository unitRepository;
    private final AuditTrailService auditTrailService;
    private final FrameworkLibraryService frameworkLibraryService;
    private final RiskMatrixService riskMatrixService;

    public OrgStructureService(OrgProfileRepository profileRepository,
                               OrgLevelConfigRepository levelRepository,
                               OrgUnitRepository unitRepository,
                               AuditTrailService auditTrailService,
                               FrameworkLibraryService frameworkLibraryService,
                               RiskMatrixService riskMatrixService) {
        this.profileRepository = profileRepository;
        this.levelRepository = levelRepository;
        this.unitRepository = unitRepository;
        this.auditTrailService = auditTrailService;
        this.frameworkLibraryService = frameworkLibraryService;
        this.riskMatrixService = riskMatrixService;
    }

    @Transactional(readOnly = true)
    public OrgProfileResponse getProfile() {
        return OrgProfileResponse.from(requireProfile());
    }

    /** Universally-readable (any authenticated tenant user): idle timeout and organisation name. */
    @Transactional(readOnly = true)
    public SessionPolicyResponse sessionPolicy() {
        OrgProfile profile = requireProfile();
        return new SessionPolicyResponse(profile.getIdleTimeoutMinutes(), profile.getDisplayName());
    }

    @Transactional
    public OrgProfileResponse updateProfile(User user, UpdateOrgProfileRequest req) {
        OrgProfile p = requireProfile();
        String oldName = p.getDisplayName();
        p.setDisplayName(blankToNull(req.displayName()));
        p.setIndustry(blankToNull(req.industry()));
        p.setSizeBand(blankToNull(req.sizeBand()));
        p.setTimezone(req.timezone().trim());
        try {
            java.time.ZoneId.of(p.getTimezone());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "timezone must be a valid IANA zone id (e.g. Asia/Dubai, Asia/Kolkata)");
        }
        p.setDateFormat(req.dateFormat().trim());
        if (req.idleTimeoutMinutes() != null) {
            int minutes = req.idleTimeoutMinutes();
            if (minutes < 1 || minutes > 480) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "idleTimeoutMinutes must be between 1 and 480");
            }
            p.setIdleTimeoutMinutes(minutes);
        }
        if (req.rtoMaxCriticalHours() != null) {
            p.setRtoMaxCriticalHours(req.rtoMaxCriticalHours());
        }
        if (req.rtoMaxHighHours() != null) {
            p.setRtoMaxHighHours(req.rtoMaxHighHours());
        }
        if (req.rtoMaxMediumHours() != null) {
            p.setRtoMaxMediumHours(req.rtoMaxMediumHours());
        }
        if (req.rtoMaxLowHours() != null) {
            p.setRtoMaxLowHours(req.rtoMaxLowHours());
        }
        if (req.riskMatrixGridSize() != null) {
            int g = req.riskMatrixGridSize();
            if (g != 3 && g != 4 && g != 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk matrix grid size must be 3, 4, or 5");
            }
            if (p.isRiskMatrixLocked() && g != p.getRiskMatrixGridSize()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Risk matrix grid size is locked after organisation setup");
            }
            if (!p.isRiskMatrixLocked()) {
                p.setRiskMatrixGridSize(g);
                riskMatrixService.ensureGridSize(g);
            }
        }
        if (req.riskAppetiteRating() != null && !req.riskAppetiteRating().isBlank()) {
            String appetite = req.riskAppetiteRating().trim().toUpperCase();
            if (p.isRiskMatrixLocked() && !appetite.equals(p.getRiskAppetiteRating())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Risk appetite is locked after organisation setup");
            }
            if (!p.isRiskMatrixLocked()) {
                p.setRiskAppetiteRating(appetite);
            }
        }
        profileRepository.save(p);
        audit(user, AuditActionType.UPDATED, "ORG_PROFILE", String.valueOf(p.getId()),
                p.getDisplayName(), "profile", oldName, p.getDisplayName());
        return OrgProfileResponse.from(p);
    }

    @Transactional(readOnly = true)
    public List<OrgLevelResponse> listLevels() {
        return levelRepository.findAllByOrderByLevelNumAsc().stream()
                .map(OrgLevelResponse::from)
                .toList();
    }

    @Transactional
    public List<OrgLevelResponse> updateLevels(User user, UpdateOrgLevelsRequest req) {
        Map<Integer, String> byLevel = new HashMap<>();
        for (UpdateOrgLevelsRequest.LevelLabel item : req.levels()) {
            if (byLevel.put(item.level(), item.label().trim()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate level in request");
            }
        }
        if (byLevel.size() != 4 || !byLevel.keySet().equals(Set.of(1, 2, 3, 4))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must supply labels for levels 1–4");
        }
        List<OrgLevelConfig> configs = levelRepository.findAllByOrderByLevelNumAsc();
        for (OrgLevelConfig c : configs) {
            String old = c.getLabel();
            String neu = byLevel.get(c.getLevelNum());
            if (!old.equals(neu)) {
                c.setLabel(neu);
                levelRepository.save(c);
                audit(user, AuditActionType.UPDATED, "ORG_LEVEL", String.valueOf(c.getLevelNum()),
                        neu, "label", old, neu);
            }
        }
        return listLevels();
    }

    @Transactional(readOnly = true)
    public List<OrgUnitResponse> tree(boolean includeInactive) {
        List<OrgUnit> all = unitRepository.findAllByOrderByLevelNumAscNameAsc();
        Map<Long, List<OrgUnit>> byParent = new HashMap<>();
        List<OrgUnit> roots = new ArrayList<>();
        for (OrgUnit u : all) {
            if (!includeInactive && !u.isActive()) {
                continue;
            }
            if (u.getParentId() == null) {
                roots.add(u);
            } else {
                byParent.computeIfAbsent(u.getParentId(), k -> new ArrayList<>()).add(u);
            }
        }
        roots.sort(Comparator.comparing(OrgUnit::getName, String.CASE_INSENSITIVE_ORDER));
        return roots.stream().map(r -> toNode(r, byParent, includeInactive)).toList();
    }

    @Transactional
    public OrgUnitResponse createUnit(User user, CreateOrgUnitRequest req) {
        String name = req.name().trim();
        int level;
        Long parentId = req.parentId();
        if (parentId == null) {
            level = 1;
        } else {
            OrgUnit parent = unitRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent unit not found"));
            if (!parent.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent unit is inactive");
            }
            level = parent.getLevelNum() + 1;
        }

        assertUniqueSiblingName(parentId, name, null);
        OrgUnit u = new OrgUnit();
        u.setName(name);
        u.setLevelNum(level);
        u.setParentId(parentId);
        u.setHeadUserId(req.headUserId());
        u.setActive(true);
        u.setSource("MANUAL");
        unitRepository.save(u);

        audit(user, AuditActionType.CREATED, "ORG_UNIT", String.valueOf(u.getId()),
                u.getName(), "name", null, u.getName());
        return OrgUnitResponse.leaf(u);
    }

    @Transactional
    public OrgUnitResponse updateUnit(User user, long id, UpdateOrgUnitRequest req) {
        OrgUnit u = requireUnit(id);
        String old = u.getName();
        String next = req.name().trim();
        assertUniqueSiblingName(u.getParentId(), next, u.getId());
        u.setName(next);
        u.setHeadUserId(req.headUserId());
        unitRepository.save(u);
        audit(user, AuditActionType.UPDATED, "ORG_UNIT", String.valueOf(u.getId()),
                u.getName(), "name", old, u.getName());
        return OrgUnitResponse.leaf(u);
    }

    @Transactional
    public OrgUnitResponse deactivateUnit(User user, long id) {
        OrgUnit u = requireUnit(id);
        if (!u.isActive()) {
            return OrgUnitResponse.leaf(u);
        }
        deactivateCascade(u);
        audit(user, AuditActionType.UPDATED, "ORG_UNIT", String.valueOf(u.getId()),
                u.getName(), "active", "true", "false");
        return OrgUnitResponse.leaf(u);
    }

    @Transactional
    public OrgUnitResponse reactivateUnit(User user, long id) {
        OrgUnit u = requireUnit(id);
        if (u.isActive()) {
            return OrgUnitResponse.leaf(u);
        }
        if (u.getParentId() != null) {
            OrgUnit parent = requireUnit(u.getParentId());
            if (!parent.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reactivate the parent unit first");
            }
        }
        u.setActive(true);
        unitRepository.save(u);
        audit(user, AuditActionType.UPDATED, "ORG_UNIT", String.valueOf(u.getId()),
                u.getName(), "active", "false", "true");
        return OrgUnitResponse.leaf(u);
    }

    /**
     * Hard-delete only when the unit has no children. Prefer deactivate when records may be tagged
     * (no module tags yet in v1: still keep the rule for future safety).
     */
    @Transactional
    public void deleteUnit(User user, long id) {
        OrgUnit u = requireUnit(id);
        List<OrgUnit> children = unitRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Unit has child units: deactivate instead, or delete children first");
        }
        String name = u.getName();
        unitRepository.delete(u);
        audit(user, AuditActionType.DELETED, "ORG_UNIT", String.valueOf(id),
                name, null, name, null);
    }

    @Transactional
    public OrgProfileResponse completeWizard(User user) {
        if (unitRepository.countByActiveTrue() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Add at least one organisation unit before completing setup");
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context required");
        }
        if (frameworkLibraryService.listActivatedForTenant(tenantId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Activate at least one framework before completing setup");
        }
        OrgProfile p = requireProfile();
        if (!p.isWizardCompleted()) {
            p.setWizardCompletedAt(Instant.now());
            p.setRiskMatrixLocked(true);
            riskMatrixService.ensureGridSize(p.getRiskMatrixGridSize());
            profileRepository.save(p);
            audit(user, AuditActionType.UPDATED, "ORG_PROFILE", String.valueOf(p.getId()),
                    p.getDisplayName(), "wizardCompleted", "false", "true");
        }
        return OrgProfileResponse.from(p);
    }

    private void deactivateCascade(OrgUnit root) {
        List<OrgUnit> stack = new ArrayList<>();
        stack.add(root);
        Set<Long> seen = new HashSet<>();
        while (!stack.isEmpty()) {
            OrgUnit cur = stack.remove(stack.size() - 1);
            if (!seen.add(cur.getId())) {
                continue;
            }
            if (cur.isActive()) {
                cur.setActive(false);
                unitRepository.save(cur);
            }
            stack.addAll(unitRepository.findByParentId(cur.getId()));
        }
    }

    private OrgUnitResponse toNode(OrgUnit u, Map<Long, List<OrgUnit>> byParent, boolean includeInactive) {
        List<OrgUnit> kids = byParent.getOrDefault(u.getId(), List.of()).stream()
                .filter(c -> includeInactive || c.isActive())
                .sorted(Comparator.comparing(OrgUnit::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<OrgUnitResponse> childNodes = kids.stream()
                .map(c -> toNode(c, byParent, includeInactive))
                .toList();
        return OrgUnitResponse.leaf(u).withChildren(childNodes);
    }

    private OrgProfile requireProfile() {
        return profileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Organisation profile not seeded"));
    }

    /**
     * Adds missing level-1 department names from Entra. Does not delete or rename existing units.
     *
     * @return how many new departments were created
     */
    @Transactional
    public int importEntraDepartments(User user, List<String> names) {
        int added = 0;
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim();
            if (name.length() > 200) {
                name = name.substring(0, 200);
            }
            if (rootNameTaken(name)) {
                continue;
            }
            OrgUnit u = new OrgUnit();
            u.setName(name);
            u.setLevelNum(1);
            u.setParentId(null);
            u.setActive(true);
            u.setSource("ENTRA");
            unitRepository.save(u);
            audit(user, AuditActionType.CREATED, "ORG_UNIT", String.valueOf(u.getId()),
                    u.getName(), "source", null, "ENTRA");
            added++;
        }
        return added;
    }

    private void assertUniqueSiblingName(Long parentId, String name, Long excludeId) {
        List<OrgUnit> siblings = parentId == null
                ? unitRepository.findByParentIdIsNull()
                : unitRepository.findByParentId(parentId);
        boolean taken = siblings.stream()
                .filter(u -> excludeId == null || !excludeId.equals(u.getId()))
                .anyMatch(u -> u.getName() != null && u.getName().trim().equalsIgnoreCase(name.trim()));
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A unit named \"" + name.trim() + "\" already exists here");
        }
    }

    private boolean rootNameTaken(String name) {
        return unitRepository.findByParentIdIsNull().stream()
                .anyMatch(u -> u.getName() != null && u.getName().trim().equalsIgnoreCase(name.trim()));
    }

    private OrgUnit requireUnit(long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation unit not found"));
    }

    private void audit(User user, AuditActionType action, String recordType, String recordId,
                       String recordName, String field, String oldVal, String newVal) {
        auditTrailService.record(
                action,
                "ORG",
                user.getId(),
                primaryRole(user),
                recordType,
                recordId,
                recordName,
                field,
                oldVal,
                newVal,
                null,
                null);
    }

    private static String primaryRole(User user) {
        return user.getRoles().stream()
                .map(Role::name)
                .sorted()
                .findFirst()
                .orElse("UNKNOWN");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
