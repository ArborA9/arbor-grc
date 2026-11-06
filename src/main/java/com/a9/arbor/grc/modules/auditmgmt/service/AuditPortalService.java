package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalEvidenceItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalGapItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalRiskItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalSessionResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalSoaItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditSummaryResponse;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceClassification;
import com.a9.arbor.grc.modules.evidence.domain.Evidence;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceVersion;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceLinkRepository;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceRepository;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceVersionRepository;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceSummaryResponse;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.soa.domain.Soa;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;
import com.a9.arbor.grc.modules.soa.domain.SoaStatus;
import com.a9.arbor.grc.modules.soa.repo.SoaItemRepository;
import com.a9.arbor.grc.modules.soa.repo.SoaRepository;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditPortalService {

    private final AuditEngagementScopeService scopeService;
    private final GapItemRepository gapItemRepository;
    private final SoaRepository soaRepository;
    private final SoaItemRepository soaItemRepository;
    private final RiskRecordRepository riskRecordRepository;
    private final EvidenceLinkRepository evidenceLinkRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceVersionRepository evidenceVersionRepository;

    public AuditPortalService(AuditEngagementScopeService scopeService,
                              GapItemRepository gapItemRepository,
                              SoaRepository soaRepository,
                              SoaItemRepository soaItemRepository,
                              RiskRecordRepository riskRecordRepository,
                              EvidenceLinkRepository evidenceLinkRepository,
                              EvidenceRepository evidenceRepository,
                              EvidenceVersionRepository evidenceVersionRepository) {
        this.scopeService = scopeService;
        this.gapItemRepository = gapItemRepository;
        this.soaRepository = soaRepository;
        this.soaItemRepository = soaItemRepository;
        this.riskRecordRepository = riskRecordRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceVersionRepository = evidenceVersionRepository;
    }

    @Transactional(readOnly = true)
    public AuditPortalSessionResponse session(User viewer) {
        AuditEngagementScopeService.ScopeContext ctx = scopeService.sessionForExternalPortal(viewer);
        GrcAudit audit = ctx.audit();
        return new AuditPortalSessionResponse(
                audit.getId(),
                audit.getName(),
                audit.getFrameworkName(),
                audit.getFrameworkCode(),
                ctx.accessExpiresAt(),
                ctx.modules(),
                List.copyOf(ctx.scopeNodeIds()),
                List.copyOf(ctx.orgUnitIds()),
                AuditSummaryResponse.from(audit));
    }

    @Transactional(readOnly = true)
    public List<AuditPortalGapItemResponse> listGapItems(User viewer, long auditId) {
        AuditEngagementScopeService.ScopeContext ctx = scopeService.requirePortalScope(viewer, auditId);
        scopeService.assertModule(ctx, EngagementScopeModule.GAP);
        Long gapAssessmentId = ctx.audit().getGapAssessmentId();
        if (gapAssessmentId == null) {
            return List.of();
        }
        List<GapItem> items = gapItemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(gapAssessmentId).stream()
                .filter(item -> scopeService.nodeInScope(ctx, item.getFrameworkNodeId()))
                .toList();
        Map<Long, Integer> evidenceCounts = evidenceCountsForGapItems(items);
        return items.stream()
                .map(item -> AuditPortalGapItemResponse.from(
                        item, effectiveStatus(item), evidenceCounts.getOrDefault(item.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditPortalSoaItemResponse> listSoaItems(User viewer, long auditId) {
        AuditEngagementScopeService.ScopeContext ctx = scopeService.requirePortalScope(viewer, auditId);
        scopeService.assertModule(ctx, EngagementScopeModule.SOA);
        Soa soa = soaRepository.findByFrameworkIdOrderByUpdatedAtDesc(ctx.audit().getFrameworkId()).stream()
                .filter(s -> s.getStatus() == SoaStatus.COMPLETE)
                .findFirst()
                .orElse(null);
        if (soa == null) {
            return List.of();
        }
        return soaItemRepository.findBySoaIdOrderByNodeCodeAsc(soa.getId()).stream()
                .filter(item -> scopeService.nodeInScope(ctx, item.getFrameworkNodeId()))
                .map(AuditPortalSoaItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditPortalRiskItemResponse> listRisks(User viewer, long auditId) {
        AuditEngagementScopeService.ScopeContext ctx = scopeService.requirePortalScope(viewer, auditId);
        scopeService.assertModule(ctx, EngagementScopeModule.RISK);
        return riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.IS_RISK).stream()
                .filter(risk -> scopeService.orgUnitInScope(ctx, risk.getOrgUnitId()))
                .filter(risk -> risk.getFrameworkNodeId() == null
                        || scopeService.nodeInScope(ctx, risk.getFrameworkNodeId()))
                .map(AuditPortalRiskItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditPortalEvidenceItemResponse> listEvidence(User viewer, long auditId) {
        AuditEngagementScopeService.ScopeContext ctx = scopeService.requirePortalScope(viewer, auditId);
        scopeService.assertModule(ctx, EngagementScopeModule.EVIDENCE);
        Set<String> allowedClassifications = allowedClassifications(viewer);
        Long gapAssessmentId = ctx.audit().getGapAssessmentId();
        if (gapAssessmentId == null) {
            return List.of();
        }
        List<GapItem> scopedItems = gapItemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(gapAssessmentId).stream()
                .filter(item -> scopeService.nodeInScope(ctx, item.getFrameworkNodeId()))
                .toList();
        if (scopedItems.isEmpty()) {
            return List.of();
        }
        Map<String, String> nodeCodeByItemId = new HashMap<>();
        for (GapItem item : scopedItems) {
            nodeCodeByItemId.put(String.valueOf(item.getId()), item.getNodeCode());
        }
        Map<Long, AuditPortalEvidenceItemResponse> deduped = new LinkedHashMap<>();
        for (GapItem item : scopedItems) {
            String recordId = String.valueOf(item.getId());
            List<EvidenceLink> links = new ArrayList<>(evidenceLinkRepository
                    .findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc("GAP", "GAP_ITEM", recordId));
            links.addAll(evidenceLinkRepository
                    .findByModuleCodeAndRecordTypeAndRecordIdStartingWithOrderByLinkedAtDesc(
                            "GAP", "GAP_QUESTION", recordId + ":"));
            for (EvidenceLink link : links) {
                deduped.computeIfAbsent(link.getEvidenceId(), evidenceId -> {
                    Evidence evidence = evidenceRepository.findById(evidenceId).orElse(null);
                    if (evidence == null || evidence.getDeletedAt() != null) {
                        return null;
                    }
                    if (!allowedClassifications.contains(evidence.getClassification().name())) {
                        return null;
                    }
                    EvidenceVersion latest = evidenceVersionRepository
                            .findByEvidenceIdAndVersionNumber(evidence.getId(), evidence.getCurrentVersion())
                            .orElse(null);
                    int linkCount = evidenceLinkRepository.findByEvidenceIdOrderByLinkedAtDesc(evidence.getId()).size();
                    EvidenceSummaryResponse summary = EvidenceSummaryResponse.from(evidence, latest, linkCount);
                    return AuditPortalEvidenceItemResponse.from(
                            summary, nodeCodeByItemId.get(recordId), "GAP_ITEM", recordId);
                });
            }
        }
        return deduped.values().stream().filter(java.util.Objects::nonNull).toList();
    }

    private Map<Long, Integer> evidenceCountsForGapItems(List<GapItem> items) {
        Map<Long, Integer> counts = new HashMap<>();
        for (GapItem item : items) {
            String recordId = String.valueOf(item.getId());
            int count = evidenceLinkRepository
                    .findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc("GAP", "GAP_ITEM", recordId)
                    .size()
                    + evidenceLinkRepository
                    .findByModuleCodeAndRecordTypeAndRecordIdStartingWithOrderByLinkedAtDesc(
                            "GAP", "GAP_QUESTION", recordId + ":")
                    .size();
            counts.put(item.getId(), count);
        }
        return counts;
    }

    private static GapImplementationStatus effectiveStatus(GapItem item) {
        return item.getFinalImplementationStatus() != null
                ? item.getFinalImplementationStatus()
                : item.getImplementationStatus();
    }

    private static Set<String> allowedClassifications(User user) {
        if (RoleAccess.isContributorOnly(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not available");
        }
        Set<EvidenceClassification> allowed = RoleAccess.isContributorOnly(user)
                ? EnumSet.of(EvidenceClassification.CONFIDENTIAL, EvidenceClassification.OPEN)
                : EnumSet.allOf(EvidenceClassification.class);
        Set<String> names = new HashSet<>();
        for (EvidenceClassification classification : allowed) {
            names.add(classification.name());
        }
        return names;
    }
}
