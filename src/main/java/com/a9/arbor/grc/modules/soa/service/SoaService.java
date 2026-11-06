package com.a9.arbor.grc.modules.soa.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.catalog.service.CloudNodeSupport;
import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.catalog.service.FrameworkQueryService;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.catalog.service.ModuleRouting;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.export.AssessmentWorkbookWriter;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.export.ReportPdfWriter;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.project.domain.ComplianceProject;
import com.a9.arbor.grc.modules.project.service.ComplianceProjectService;
import com.a9.arbor.grc.modules.soa.domain.Soa;
import com.a9.arbor.grc.modules.soa.domain.SoaApplicability;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;
import com.a9.arbor.grc.modules.soa.domain.SoaStatus;
import com.a9.arbor.grc.modules.soa.repo.SoaItemRepository;
import com.a9.arbor.grc.modules.soa.repo.SoaRepository;
import com.a9.arbor.grc.modules.soa.web.dto.BulkUpdateSoaItemsRequest;
import com.a9.arbor.grc.modules.soa.web.dto.CreateSoaRequest;
import com.a9.arbor.grc.modules.soa.web.dto.SoaDetailResponse;
import com.a9.arbor.grc.modules.soa.web.dto.SoaItemResponse;
import com.a9.arbor.grc.modules.soa.web.dto.SoaSummaryResponse;
import com.a9.arbor.grc.modules.soa.web.dto.UpdateSoaItemRequest;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SoaService {

    private final SoaRepository soaRepository;
    private final SoaItemRepository itemRepository;
    private final FrameworkLibraryService frameworkLibraryService;
    private final FrameworkQueryService frameworkQueryService;
    private final ComplianceProjectService projectService;
    private final AuditTrailService auditTrailService;
    private final AssessmentWorkbookWriter workbookWriter;
    private final ReportPdfWriter pdfWriter;
    private final TenantRepository tenantRepository;
    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;

    public SoaService(SoaRepository soaRepository,
                      SoaItemRepository itemRepository,
                      FrameworkLibraryService frameworkLibraryService,
                      FrameworkQueryService frameworkQueryService,
                      ComplianceProjectService projectService,
                      AuditTrailService auditTrailService,
                      AssessmentWorkbookWriter workbookWriter,
                      ReportPdfWriter pdfWriter,
                      TenantRepository tenantRepository,
                      ModuleLicensingService moduleLicensingService,
                      AdminModuleAccessService adminModuleAccessService) {
        this.soaRepository = soaRepository;
        this.itemRepository = itemRepository;
        this.frameworkLibraryService = frameworkLibraryService;
        this.frameworkQueryService = frameworkQueryService;
        this.projectService = projectService;
        this.auditTrailService = auditTrailService;
        this.workbookWriter = workbookWriter;
        this.pdfWriter = pdfWriter;
        this.tenantRepository = tenantRepository;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    @Transactional(readOnly = true)
    public List<SoaSummaryResponse> list(Long projectId) {
        List<Soa> rows = projectId == null
                ? soaRepository.findAllByOrderByUpdatedAtDesc()
                : soaRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        return rows.stream()
                .filter(s -> adminModuleAccessService.mayUseFramework(s.getFrameworkId(), false))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SoaSummaryResponse> listByProject(long projectId) {
        return list(projectId);
    }

    @Transactional(readOnly = true)
    public SoaDetailResponse get(long id) {
        Soa soa = require(id);
        if (!adminModuleAccessService.mayUseFramework(soa.getFrameworkId(), false)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement of Applicability not found");
        }
        Map<Long, FrameworkNodeResponse> nodes = nodesById(soa.getFrameworkId());
        List<SoaItemResponse> items = itemsInExcelOrder(id, nodes).stream()
                .map(item -> toItemResponse(item, nodes.get(item.getFrameworkNodeId())))
                .toList();
        return new SoaDetailResponse(toSummary(soa), items);
    }

    @Transactional
    public SoaDetailResponse create(User user, CreateSoaRequest req) {
        String tenantId = requireTenant();
        ComplianceProject project = projectService.requireActive(req.projectId());
        projectService.assertFrameworkInProject(req.projectId(), req.frameworkId());
        if (soaRepository.existsByProjectIdAndFrameworkId(req.projectId(), req.frameworkId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A SoA already exists for this framework in the project");
        }
        FrameworkDetailResponse detail = frameworkLibraryService
                .getActivatedControls(tenantId, req.frameworkId(), ModuleRouting.SOA);
        FrameworkSummaryResponse fw = detail.framework();
        moduleLicensingService.requireWritableFramework(tenantId, fw.id());
        if (!adminModuleAccessService.mayUseFramework(fw.id(), true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this module");
        }
        boolean includeCloud = project.isIncludeCloud();

        Soa soa = new Soa();
        soa.setProjectId(req.projectId());
        soa.setFrameworkId(fw.id());
        soa.setFrameworkCode(fw.code());
        soa.setFrameworkName(fw.name());
        soa.setFrameworkVersion(fw.version());
        soa.setStatus(SoaStatus.DRAFT);
        soa.setCreatedByUserId(user.getId());
        soaRepository.save(soa);

        for (FrameworkNodeResponse node : detail.nodes()) {
            if (!includeCloud && CloudNodeSupport.isCloud(node)) {
                continue;
            }
            SoaItem item = new SoaItem();
            item.setSoaId(soa.getId());
            item.setFrameworkNodeId(node.id());
            item.setNodeCode(node.code());
            item.setTitle(node.title());
            item.setDefinitionText(node.definition());
            if (CloudNodeSupport.isApplicabilityLocked(node)) {
                item.setApplicability(SoaApplicability.APPLICABLE);
                item.setJustification(lockedJustification(node));
                item.setUpdatedByUserId(user.getId());
            }
            itemRepository.save(item);
        }

        audit(user, AuditActionType.CREATED, "SOA", String.valueOf(soa.getId()),
                soa.getFrameworkCode(), null, null, soa.getFrameworkName());
        return get(soa.getId());
    }

    @Transactional
    public SoaItemResponse updateItem(User user, long soaId, long itemId, UpdateSoaItemRequest req) {
        Soa soa = require(soaId);
        assertEditable(soa);
        SoaItem item = itemRepository.findByIdAndSoaId(itemId, soaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SoA item not found"));
        FrameworkNodeResponse node = nodesById(soa.getFrameworkId()).get(item.getFrameworkNodeId());
        assertApplicabilityChangeAllowed(node, req.applicability());

        String oldApp = item.getApplicability() != null ? item.getApplicability().name() : null;
        item.setApplicability(req.applicability());
        String justification = req.justification() != null ? req.justification().trim() : "";
        if (req.applicability() == SoaApplicability.NOT_APPLICABLE && justification.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Justification is required when marking Not applicable");
        }
        item.setJustification(justification);
        item.setRemarks(blankToNull(req.remarks()));
        item.setUpdatedByUserId(user.getId());
        itemRepository.save(item);
        soaRepository.save(soa);

        audit(user, AuditActionType.UPDATED, "SOA_ITEM", String.valueOf(item.getId()),
                item.getNodeCode(), "applicability", oldApp, req.applicability().name());
        return toItemResponse(item, node);
    }

    /**
     * Bulk-set applicability + justification. Default product use: UNANSWERED + a shared justification
     * so admins can first-pass a large SoA without clicking every row.
     */
    @Transactional
    public SoaDetailResponse bulkUpdateItems(User user, long soaId, BulkUpdateSoaItemsRequest req) {
        Soa soa = require(soaId);
        assertEditable(soa);
        String justification = req.justification() != null ? req.justification().trim() : "";
        if (req.applicability() == SoaApplicability.NOT_APPLICABLE && justification.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Justification is required when marking Not applicable");
        }
        if (req.applicability() == SoaApplicability.APPLICABLE && justification.isEmpty()) {
            justification = "Bulk marked Applicable";
        }

        List<SoaItem> items = itemRepository.findBySoaIdOrderByNodeCodeAsc(soaId);
        Map<Long, FrameworkNodeResponse> nodes = nodesById(soa.getFrameworkId());
        Set<Long> selectedIds = null;
        if (req.scope() == BulkUpdateSoaItemsRequest.BulkSoaScope.SELECTED) {
            if (req.itemIds() == null || req.itemIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "itemIds required when scope is SELECTED");
            }
            selectedIds = new HashSet<>(req.itemIds());
        }
        int updated = 0;
        int skippedLocked = 0;
        for (SoaItem item : items) {
            boolean unanswered = item.getApplicability() == null;
            if (req.scope() == BulkUpdateSoaItemsRequest.BulkSoaScope.UNANSWERED && !unanswered) {
                continue;
            }
            if (req.scope() == BulkUpdateSoaItemsRequest.BulkSoaScope.SELECTED
                    && (selectedIds == null || !selectedIds.contains(item.getId()))) {
                continue;
            }
            FrameworkNodeResponse node = nodes.get(item.getFrameworkNodeId());
            if (CloudNodeSupport.isApplicabilityLocked(node)) {
                if (req.applicability() == SoaApplicability.NOT_APPLICABLE) {
                    skippedLocked++;
                    continue;
                }
                // Locked rows stay Applicable; still refresh justification if marking Applicable.
            }
            String oldApp = item.getApplicability() != null ? item.getApplicability().name() : null;
            item.setApplicability(req.applicability());
            item.setJustification(justification);
            item.setUpdatedByUserId(user.getId());
            itemRepository.save(item);
            updated++;
            if (updated <= 5) {
                audit(user, AuditActionType.UPDATED, "SOA_ITEM", String.valueOf(item.getId()),
                        item.getNodeCode(), "applicability", oldApp, req.applicability().name());
            }
        }
        if (req.scope() == BulkUpdateSoaItemsRequest.BulkSoaScope.SELECTED && updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    skippedLocked > 0
                            ? "Selected items are locked (clauses / Always applicable): cannot mark N/A"
                            : "No matching SoA items for the selected ids");
        }
        soaRepository.save(soa);
        audit(user, AuditActionType.UPDATED, "SOA", String.valueOf(soa.getId()),
                soa.getFrameworkCode(), "bulk_applicability", req.scope().name(),
                req.applicability().name() + " x" + updated
                        + (skippedLocked > 0 ? " (skipped locked " + skippedLocked + ")" : ""));
        return get(soaId);
    }

    @Transactional
    public SoaDetailResponse complete(User user, long id) {
        Soa soa = require(id);
        assertFrameworkWritable(soa.getFrameworkId());
        if (soa.getStatus() == SoaStatus.COMPLETE) {
            return get(id);
        }
        List<SoaItem> items = itemRepository.findBySoaIdOrderByNodeCodeAsc(id);
        List<String> missing = items.stream()
                .filter(i -> i.getApplicability() == null
                        || (i.getApplicability() == SoaApplicability.NOT_APPLICABLE
                        && (i.getJustification() == null || i.getJustification().isBlank())))
                .map(SoaItem::getNodeCode)
                .limit(10)
                .toList();
        if (!missing.isEmpty()) {
            long incomplete = items.stream()
                    .filter(i -> i.getApplicability() == null
                            || (i.getApplicability() == SoaApplicability.NOT_APPLICABLE
                            && (i.getJustification() == null || i.getJustification().isBlank())))
                    .count();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Every control needs applicability; Not applicable rows also need justification. Incomplete examples: "
                            + String.join(", ", missing)
                            + (incomplete > 10 ? "…" : ""));
        }
        soa.setStatus(SoaStatus.COMPLETE);
        soa.setCompletedAt(Instant.now());
        soa.setCompletedByUserId(user.getId());
        soaRepository.save(soa);
        audit(user, AuditActionType.UPDATED, "SOA", String.valueOf(soa.getId()),
                soa.getFrameworkCode(), "status", "DRAFT", "COMPLETE");
        return get(id);
    }

    @Transactional
    public SoaDetailResponse reopen(User user, long id) {
        Soa soa = require(id);
        assertFrameworkWritable(soa.getFrameworkId());
        if (soa.getStatus() == SoaStatus.DRAFT) {
            return get(id);
        }
        soa.setStatus(SoaStatus.DRAFT);
        soa.setCompletedAt(null);
        soa.setCompletedByUserId(null);
        soaRepository.save(soa);
        audit(user, AuditActionType.UPDATED, "SOA", String.valueOf(soa.getId()),
                soa.getFrameworkCode(), "status", "COMPLETE", "DRAFT");
        return get(id);
    }

    @Transactional(readOnly = true)
    public Soa requireCompleteForProjectFramework(Long projectId, Long frameworkId) {
        return findCompleteForProjectFramework(projectId, frameworkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Complete a SoA for this framework in the project before creating a Gap Assessment"));
    }

    /** Optional complete SoA for Gap Option A: Gap may run without SoA. */
    @Transactional(readOnly = true)
    public java.util.Optional<Soa> findCompleteForProjectFramework(Long projectId, Long frameworkId) {
        return soaRepository.findByProjectIdAndFrameworkId(projectId, frameworkId)
                .filter(soa -> soa.getStatus() == SoaStatus.COMPLETE);
    }

    @Transactional(readOnly = true)
    public List<SoaItem> itemsForSoa(long soaId) {
        return itemRepository.findBySoaIdOrderByNodeCodeAsc(soaId);
    }

    /**
     * Excel export for COMPLETE SoA only (Path B6). OWNER/ADMIN/INTERNAL_AUDITOR/EXTERNAL_AUDITOR: not Contributor/Global Reader.
     */
    @Transactional
    public ExportFile exportExcel(User user, long id) {
        Soa soa = requireCompleteForExport(id);
        Map<Long, FrameworkNodeResponse> nodes = nodesById(soa.getFrameworkId());
        List<SoaItem> items = itemsInExcelOrder(id, nodes);
        byte[] bytes = workbookWriter.writeSoa(soa, items);
        String filename = "SoA-" + safeFilePart(soa.getFrameworkCode()) + "-" + soa.getId() + ".xlsx";
        audit(user, AuditActionType.EXPORT, "SOA", String.valueOf(soa.getId()),
                soa.getFrameworkCode(), "export", null, "XLSX");
        return new ExportFile(bytes, filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /** PDF export for COMPLETE SoA only (§19.3). Same role gate as Excel. */
    @Transactional
    public ExportFile exportPdf(User user, long id) {
        Soa soa = requireCompleteForExport(id);
        Map<Long, FrameworkNodeResponse> nodes = nodesById(soa.getFrameworkId());
        List<SoaItem> items = itemsInExcelOrder(id, nodes);
        byte[] bytes = pdfWriter.writeSoa(orgName(), user, soa, items);
        String filename = "SoA-" + safeFilePart(soa.getFrameworkCode()) + "-" + soa.getId() + ".pdf";
        audit(user, AuditActionType.EXPORT, "SOA", String.valueOf(soa.getId()),
                soa.getFrameworkCode(), "export", null, "PDF");
        return new ExportFile(bytes, filename, "application/pdf");
    }

    private Soa requireCompleteForExport(long id) {
        Soa soa = require(id);
        assertFrameworkWritable(soa.getFrameworkId());
        if (soa.getStatus() != SoaStatus.COMPLETE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "SoA must be COMPLETE before export");
        }
        return soa;
    }

    private String orgName() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return "-";
        }
        return tenantRepository.findByTenantId(tenantId).map(Tenant::getName).orElse(tenantId);
    }

    private static String safeFilePart(String raw) {
        if (raw == null || raw.isBlank()) {
            return "soa";
        }
        String s = raw.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        return s.isBlank() ? "soa" : s.substring(0, Math.min(s.length(), 64));
    }

    private SoaSummaryResponse toSummary(Soa soa) {
        List<SoaItem> items = itemRepository.findBySoaIdOrderByNodeCodeAsc(soa.getId());
        int applicable = 0;
        int notApplicable = 0;
        int unanswered = 0;
        for (SoaItem i : items) {
            if (i.getApplicability() == SoaApplicability.APPLICABLE) {
                applicable++;
            } else if (i.getApplicability() == SoaApplicability.NOT_APPLICABLE) {
                notApplicable++;
            } else {
                unanswered++;
            }
        }
        return SoaSummaryResponse.from(soa, items.size(), applicable, notApplicable, unanswered);
    }

    private Soa require(long id) {
        return soaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SoA not found"));
    }

    private void assertEditable(Soa soa) {
        assertFrameworkWritable(soa.getFrameworkId());
        if (soa.getStatus() == SoaStatus.COMPLETE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SoA is complete: reopen to edit");
        }
    }

    private void assertFrameworkWritable(Long frameworkId) {
        if (frameworkId == null) {
            return;
        }
        moduleLicensingService.requireWritableFramework(requireTenant(), frameworkId);
        if (!adminModuleAccessService.mayUseFramework(frameworkId, true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this module");
        }
    }

    private Map<Long, FrameworkNodeResponse> nodesById(long frameworkId) {
        return frameworkLibraryService.getDetail(frameworkId).nodes().stream()
                .collect(Collectors.toMap(FrameworkNodeResponse::id, Function.identity(), (a, b) -> a));
    }

    /** Catalog {@code displayOrder} (Excel row order), not lexicographic node codes. */
    private List<SoaItem> itemsInExcelOrder(long soaId, Map<Long, FrameworkNodeResponse> nodes) {
        return itemRepository.findBySoaIdOrderByNodeCodeAsc(soaId).stream()
                .sorted(Comparator
                        .comparingInt((SoaItem item) -> {
                            FrameworkNodeResponse node = nodes.get(item.getFrameworkNodeId());
                            return node != null ? node.displayOrder() : Integer.MAX_VALUE;
                        })
                        .thenComparing(SoaItem::getNodeCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private static SoaItemResponse toItemResponse(SoaItem item, FrameworkNodeResponse node) {
        return SoaItemResponse.from(
                item,
                node != null ? node.priority() : null,
                node != null ? node.recordType() : null,
                CloudNodeSupport.isApplicabilityLocked(node));
    }

    private static void assertApplicabilityChangeAllowed(FrameworkNodeResponse node,
                                                         SoaApplicability applicability) {
        if (CloudNodeSupport.isApplicabilityLocked(node)
                && applicability == SoaApplicability.NOT_APPLICABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Clauses and Always-applicable controls cannot be marked Not applicable");
        }
    }

    private static String lockedJustification(FrameworkNodeResponse node) {
        if ("ALWAYS".equalsIgnoreCase(CloudNodeSupport.attr(node, "default_applicability"))) {
            return "Always applicable per framework";
        }
        return "Mandatory clause: always applicable";
    }

    private static String requireTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context required");
        }
        return tenantId;
    }

    private void audit(User user, AuditActionType action, String recordType, String recordId,
                       String recordName, String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "SOA", user.getId(), primaryRole(user),
                recordType, recordId, recordName, field, oldVal, newVal, null, null);
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
}
