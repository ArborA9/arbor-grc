package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessGrant;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditGranteeType;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;
import com.a9.arbor.grc.modules.auditmgmt.repo.AuditAccessGrantRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditOrgUnitRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditScopeModuleRepository;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AccessGrantResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateAccessGrantRequest;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class AuditAccessGrantService {

    private final AuditAccessGrantRepository grantRepository;
    private final GrcAuditRepository auditRepository;
    private final GrcAuditScopeModuleRepository scopeModuleRepository;
    private final GrcAuditOrgUnitRepository auditOrgUnitRepository;
    private final UserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AuditRefGenerator refGenerator;
    private final AuditAccessLogService accessLogService;
    private final ObjectMapper objectMapper;

    public AuditAccessGrantService(AuditAccessGrantRepository grantRepository,
                                   GrcAuditRepository auditRepository,
                                   GrcAuditScopeModuleRepository scopeModuleRepository,
                                   GrcAuditOrgUnitRepository auditOrgUnitRepository,
                                   UserRepository userRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   AuditRefGenerator refGenerator,
                                   AuditAccessLogService accessLogService,
                                   ObjectMapper objectMapper) {
        this.grantRepository = grantRepository;
        this.auditRepository = auditRepository;
        this.scopeModuleRepository = scopeModuleRepository;
        this.auditOrgUnitRepository = auditOrgUnitRepository;
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.refGenerator = refGenerator;
        this.accessLogService = accessLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AccessGrantResponse> listAll() {
        return grantRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccessGrantResponse> listForAudit(long auditId) {
        return grantRepository.findByAuditIdOrderByIdAsc(auditId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> activeAuditIdsForUser(User viewer) {
        LocalDate today = LocalDate.now();
        return grantRepository.findByUserIdOrderByUpdatedAtDesc(viewer.getId()).stream()
                .filter(g -> g.activeOn(today))
                .map(AuditAccessGrant::getAuditId)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAccess(User viewer, long auditId) {
        return grantRepository.findByAuditIdAndUserIdAndRevokedFalse(auditId, viewer.getId())
                .filter(g -> g.activeOn(LocalDate.now()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Set<EngagementScopeModule> grantedModulesForUser(User viewer) {
        EnumSet<EngagementScopeModule> out = EnumSet.noneOf(EngagementScopeModule.class);
        LocalDate today = LocalDate.now();
        for (AuditAccessGrant g : grantRepository.findByUserIdOrderByUpdatedAtDesc(viewer.getId())) {
            if (!g.activeOn(today)) {
                continue;
            }
            out.addAll(readModules(g.getModulesJson()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<AccessGrantResponse> activeGrantResponsesForUser(User viewer) {
        LocalDate today = LocalDate.now();
        return grantRepository.findByUserIdOrderByUpdatedAtDesc(viewer.getId()).stream()
                .filter(g -> g.activeOn(today))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccessGrantResponse create(User actor, long auditId, CreateAccessGrantRequest req) {
        GrcAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
        User grantee = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grantee not found"));

        if (grantRepository.existsByAuditIdAndUserIdAndRevokedFalse(auditId, grantee.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active access grant already exists for this person on this audit");
        }

        assertExternalFields(req);

        List<EngagementScopeModule> modules = req.modules() == null || req.modules().isEmpty()
                ? modulesFromAudit(auditId)
                : req.modules();
        LocalDate from = req.validFrom() != null ? req.validFrom()
                : (audit.getStartDate() != null ? audit.getStartDate() : LocalDate.now());
        LocalDate to = req.validTo() != null ? req.validTo()
                : (audit.getEndDate() != null ? audit.getEndDate() : from.plusDays(14));
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid to must be on or after valid from");
        }

        AuditAccessGrant g = new AuditAccessGrant();
        g.setDisplayId(refGenerator.nextGrantId());
        g.setAuditId(auditId);
        g.setUserId(grantee.getId());
        g.setGranteeType(req.granteeType());
        g.setOrganisation(blankToNull(req.organisation()));
        g.setNdaReference(blankToNull(req.ndaReference()));
        g.setModulesJson(writeModules(modules));
        g.setScopeBoundary(boundaryLabel(audit));
        g.setCanPreviewEvidence(req.canPreviewEvidence() == null || req.canPreviewEvidence());
        g.setCanExportNotes(req.canExportNotes() == null || req.canExportNotes());
        g.setCanDownloadEvidence(Boolean.TRUE.equals(req.canDownloadEvidence()));
        g.setValidFrom(from);
        g.setValidTo(to);
        g.setGraceDays(req.graceDays() != null ? req.graceDays() : Math.max(audit.getGraceDays(), 0));
        grantRepository.save(g);

        if (audit.getStatus() == GrcAuditStatus.DRAFT) {
            audit.setStatus(GrcAuditStatus.ACCESS_GRANTED);
            auditRepository.save(audit);
        }
        return toResponse(g);
    }

    @Transactional
    public void grantTeam(GrcAudit audit, User grantee, AuditGranteeType type) {
        if (grantRepository.existsByAuditIdAndUserIdAndRevokedFalse(audit.getId(), grantee.getId())) {
            return;
        }
        if (type == AuditGranteeType.EXTERNAL_AUDITOR || type == AuditGranteeType.TECHNICAL_EXPERT) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate from = audit.getStartDate() != null ? audit.getStartDate() : today;
        if (from.isAfter(today)) {
            from = today;
        }
        LocalDate to = audit.getEndDate() != null ? audit.getEndDate() : from.plusDays(14);
        if (to.isBefore(from)) {
            to = from.plusDays(14);
        }
        AuditAccessGrant g = new AuditAccessGrant();
        g.setDisplayId(refGenerator.nextGrantId());
        g.setAuditId(audit.getId());
        g.setUserId(grantee.getId());
        g.setGranteeType(type);
        g.setModulesJson(writeModules(modulesFromAudit(audit.getId())));
        g.setScopeBoundary(boundaryLabel(audit));
        g.setCanPreviewEvidence(true);
        g.setCanExportNotes(true);
        g.setCanDownloadEvidence(false);
        g.setValidFrom(from);
        g.setValidTo(to);
        g.setGraceDays(Math.max(audit.getGraceDays(), 0));
        grantRepository.save(g);
        if (audit.getStatus() == GrcAuditStatus.DRAFT) {
            audit.setStatus(GrcAuditStatus.ACCESS_GRANTED);
            auditRepository.save(audit);
        }
    }

    @Transactional
    public AccessGrantResponse revoke(User actor, long grantId, String reason) {
        AuditAccessGrant g = grantRepository.findById(grantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Access grant not found"));
        if (g.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Grant already revoked");
        }
        g.setRevoked(true);
        g.setRevokedReason(reason.trim());
        g.setRevokedAt(Instant.now());
        g.setRevokedByUserId(actor.getId());
        grantRepository.save(g);
        return toResponse(g);
    }

    @Transactional
    public void syncModulesFromAudit(long auditId) {
        String json = writeModules(modulesFromAudit(auditId));
        String boundary = auditRepository.findById(auditId).map(this::boundaryLabel).orElse("");
        for (AuditAccessGrant g : grantRepository.findByAuditIdOrderByIdAsc(auditId)) {
            if (g.isRevoked()) {
                continue;
            }
            g.setModulesJson(json);
            g.setScopeBoundary(boundary);
            grantRepository.save(g);
        }
    }

    public AccessGrantResponse toResponse(AuditAccessGrant g) {
        return AccessGrantResponse.from(
                g, readModules(g.getModulesJson()), accessStatus(g),
                independenceResult(g.getGranteeType()),
                accessLogService.countForGrant(g.getId()));
    }

    private void assertExternalFields(CreateAccessGrantRequest req) {
        if (req.granteeType() == AuditGranteeType.EXTERNAL_AUDITOR
                || req.granteeType() == AuditGranteeType.TECHNICAL_EXPERT) {
            if (req.organisation() == null || req.organisation().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Organisation is required for an external grantee");
            }
            if (req.granteeType() == AuditGranteeType.EXTERNAL_AUDITOR
                    && (req.ndaReference() == null || req.ndaReference().isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "NDA reference is required before an external auditor grant can be issued");
            }
        }
    }

    String independenceResult(AuditGranteeType type) {
        if (type != AuditGranteeType.INTERNAL_AUDITOR) {
            return "n/a - external";
        }
        return "PASS";
    }

    private Long auditeeUnitId(GrcAudit audit) {
        if (audit.getAuditeeOrgUnitId() != null) {
            return audit.getAuditeeOrgUnitId();
        }
        return auditOrgUnitRepository.findByAuditId(audit.getId()).stream()
                .map(link -> link.getOrgUnitId())
                .findFirst()
                .orElse(null);
    }

    private List<EngagementScopeModule> modulesFromAudit(long auditId) {
        List<EngagementScopeModule> stored = scopeModuleRepository.findByAuditId(auditId).stream()
                .map(GrcAuditScopeModule::getModuleCode)
                .toList();
        return stored.isEmpty()
                ? List.of(EngagementScopeModule.GAP, EngagementScopeModule.SOA, EngagementScopeModule.RISK)
                : stored;
    }

    private String boundaryLabel(GrcAudit audit) {
        Long unitId = auditeeUnitId(audit);
        if (unitId == null) {
            return "All divisions";
        }
        return orgUnitRepository.findById(unitId).map(OrgUnit::getName).orElse("Org unit " + unitId);
    }

    private String accessStatus(AuditAccessGrant g) {
        if (g.isRevoked()) {
            return "REVOKED";
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(g.getValidFrom())) {
            return "Pending";
        }
        LocalDate expiry = g.autoExpiry();
        if (today.isAfter(expiry)) {
            return "EXPIRED";
        }
        if (!today.isAfter(expiry.minusDays(3))) {
            return "Active";
        }
        return "Expiring";
    }

    private String writeModules(List<EngagementScopeModule> modules) {
        try {
            return objectMapper.writeValueAsString(modules);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<EngagementScopeModule> readModules(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<EngagementScopeModule>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
