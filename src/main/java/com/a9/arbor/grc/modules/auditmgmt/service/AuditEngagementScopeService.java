package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.ExternalAccessStatus;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditExternalAccess;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditOrgUnit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditScopeNode;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditExternalAccessRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditOrgUnitRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditScopeModuleRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditScopeNodeRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditTeamMemberRepository;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuditEngagementScopeService {

    public record ScopeContext(
            GrcAudit audit,
            List<EngagementScopeModule> modules,
            Set<Long> scopeNodeIds,
            Set<Long> orgUnitIds,
            Instant accessExpiresAt
    ) {
    }

    private final GrcAuditRepository auditRepository;
    private final GrcAuditOrgUnitRepository orgUnitLinkRepository;
    private final GrcAuditScopeModuleRepository scopeModuleRepository;
    private final GrcAuditScopeNodeRepository scopeNodeRepository;
    private final GrcAuditExternalAccessRepository externalAccessRepository;
    private final GrcAuditTeamMemberRepository teamMemberRepository;
    private final AuditExternalAccessService externalAccessService;

    public AuditEngagementScopeService(GrcAuditRepository auditRepository,
                                       GrcAuditOrgUnitRepository orgUnitLinkRepository,
                                       GrcAuditScopeModuleRepository scopeModuleRepository,
                                       GrcAuditScopeNodeRepository scopeNodeRepository,
                                       GrcAuditExternalAccessRepository externalAccessRepository,
                                       GrcAuditTeamMemberRepository teamMemberRepository,
                                       AuditExternalAccessService externalAccessService) {
        this.auditRepository = auditRepository;
        this.orgUnitLinkRepository = orgUnitLinkRepository;
        this.scopeModuleRepository = scopeModuleRepository;
        this.scopeNodeRepository = scopeNodeRepository;
        this.externalAccessRepository = externalAccessRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.externalAccessService = externalAccessService;
    }

    @Transactional(readOnly = true)
    public ScopeContext sessionForExternalPortal(User viewer) {
        if (!isExternalOnly(viewer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "External auditor portal only");
        }
        GrcAuditExternalAccess access = externalAccessRepository
                .findFirstByUserIdAndStatusOrderByRedeemedAtDesc(viewer.getId(), ExternalAccessStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active audit access"));
        if (access.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Access expired");
        }
        GrcAudit audit = auditRepository.findById(access.getAuditId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
        return loadScope(audit, access.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public ScopeContext requirePortalScope(User viewer, long auditId) {
        GrcAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
        assertCanViewEngagement(viewer, audit);
        Instant expiresAt = externalAccessRepository
                .findFirstByUserIdAndAuditIdAndStatusOrderByRedeemedAtDesc(
                        viewer.getId(), auditId, ExternalAccessStatus.ACTIVE)
                .map(GrcAuditExternalAccess::getExpiresAt)
                .orElse(null);
        return loadScope(audit, expiresAt);
    }

    public void assertModule(ScopeContext ctx, EngagementScopeModule module) {
        if (!ctx.modules().contains(module)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module not in engagement scope: " + module.name());
        }
    }

    public boolean nodeInScope(ScopeContext ctx, Long frameworkNodeId) {
        if (frameworkNodeId == null) {
            return false;
        }
        if (ctx.scopeNodeIds().isEmpty()) {
            return true;
        }
        return ctx.scopeNodeIds().contains(frameworkNodeId);
    }

    public boolean orgUnitInScope(ScopeContext ctx, Long orgUnitId) {
        if (orgUnitId == null) {
            return ctx.orgUnitIds().isEmpty();
        }
        if (ctx.orgUnitIds().isEmpty()) {
            return true;
        }
        return ctx.orgUnitIds().contains(orgUnitId);
    }

    private ScopeContext loadScope(GrcAudit audit, Instant accessExpiresAt) {
        List<EngagementScopeModule> modules = scopeModuleRepository.findByAuditId(audit.getId()).stream()
                .map(GrcAuditScopeModule::getModuleCode)
                .toList();
        Set<Long> scopeNodeIds = scopeNodeRepository.findByAuditId(audit.getId()).stream()
                .map(GrcAuditScopeNode::getFrameworkNodeId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<Long> orgUnitIds = orgUnitLinkRepository.findByAuditId(audit.getId()).stream()
                .map(GrcAuditOrgUnit::getOrgUnitId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        return new ScopeContext(audit, modules, scopeNodeIds, orgUnitIds, accessExpiresAt);
    }

    private void assertCanViewEngagement(User viewer, GrcAudit audit) {
        if (isAdmin(viewer) || viewer.getRoles().contains(Role.GLOBAL_READER)) {
            return;
        }
        if (viewer.getId().equals(audit.getLeadAuditorUserId())) {
            return;
        }
        if (externalAccessService.hasActiveAccess(viewer.getId(), audit.getId())) {
            return;
        }
        if (teamMemberRepository.existsByAuditIdAndUserId(audit.getId(), viewer.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Scoped to assigned audits only");
    }

    private static boolean isExternalOnly(User user) {
        return user.getRoles().contains(Role.EXTERNAL_AUDITOR)
                && user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN
                || r == Role.INTERNAL_AUDITOR || r == Role.GLOBAL_READER);
    }

    private static boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
    }
}
