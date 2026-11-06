package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborAuditProperties;
import com.a9.arbor.grc.modules.auditmgmt.domain.ExternalAccessStatus;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklist;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditTeamMember;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditChecklistItemRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditChecklistRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditExternalAccessRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditFindingRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditScopeModuleRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditScopeNodeRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditTeamMemberRepository;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditDomainNarrativeResponse;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.repo.CapaRepository;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditReportService {

    private static final String PDF = "application/pdf";

    private final GrcAuditRepository auditRepository;
    private final GrcAuditFindingRepository findingRepository;
    private final GrcAuditChecklistItemRepository checklistRepository;
    private final GrcAuditChecklistRepository checklistHeaderRepository;
    private final GrcAuditTeamMemberRepository teamMemberRepository;
    private final GrcAuditScopeModuleRepository scopeModuleRepository;
    private final GrcAuditScopeNodeRepository scopeNodeRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final CapaRepository capaRepository;
    private final AuditReportPdfWriter pdfWriter;
    private final AuditReportExcelWriter excelWriter;
    private final AuditReportStore reportStore;
    private final GrcAuditExternalAccessRepository externalAccessRepository;
    private final ArborAuditProperties auditProperties;

    public AuditReportService(GrcAuditRepository auditRepository,
                              GrcAuditFindingRepository findingRepository,
                              GrcAuditChecklistItemRepository checklistRepository,
                              GrcAuditChecklistRepository checklistHeaderRepository,
                              GrcAuditTeamMemberRepository teamMemberRepository,
                              GrcAuditScopeModuleRepository scopeModuleRepository,
                              GrcAuditScopeNodeRepository scopeNodeRepository,
                              UserRepository userRepository,
                              TenantRepository tenantRepository,
                              OrgUnitRepository orgUnitRepository,
                              CapaRepository capaRepository,
                              AuditReportPdfWriter pdfWriter,
                              AuditReportExcelWriter excelWriter,
                              AuditReportStore reportStore,
                              GrcAuditExternalAccessRepository externalAccessRepository,
                              ArborAuditProperties auditProperties) {
        this.auditRepository = auditRepository;
        this.findingRepository = findingRepository;
        this.checklistRepository = checklistRepository;
        this.checklistHeaderRepository = checklistHeaderRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.scopeModuleRepository = scopeModuleRepository;
        this.scopeNodeRepository = scopeNodeRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.capaRepository = capaRepository;
        this.pdfWriter = pdfWriter;
        this.excelWriter = excelWriter;
        this.reportStore = reportStore;
        this.externalAccessRepository = externalAccessRepository;
        this.auditProperties = auditProperties;
    }

    @Transactional
    public void generateOnLock(GrcAudit audit) {
        if (!audit.isLocked() || audit.getReportStorageKey() != null) {
            return;
        }
        User admin = audit.getAdminSignedByUserId() == null
                ? null
                : userRepository.findById(audit.getAdminSignedByUserId()).orElse(null);
        String generatedBy = admin == null ? "Arbor GRC" : personLine(admin);
        try {
            byte[] pdf = pdfWriter.write(assemble(audit, generatedBy, true));
            AuditReportStore.StoredReport stored = reportStore.store(audit.getId(), pdf);
            audit.setReportStorageKey(stored.storageKey());
            audit.setReportGeneratedAt(Instant.now());
            auditRepository.save(audit);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not export the audit report", e);
        }
    }

    /** Live PDF of the audit project. Does not require the audit to be closed. */
    @Transactional(readOnly = true)
    public ExportFile download(User viewer, long auditId) {
        return pdf(viewer, auditId);
    }

    @Transactional(readOnly = true)
    public ExportFile pdf(User viewer, long auditId) {
        GrcAudit audit = require(auditId);
        assertCanDownload(viewer, audit);
        try {
            byte[] bytes = pdfWriter.write(assemble(audit, personLine(viewer), includeNotes(audit, viewer)));
            return new ExportFile(bytes, fileBase(audit) + "-report.pdf", PDF);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not export the audit report", e);
        }
    }

    @Transactional(readOnly = true)
    public ExportFile excel(User viewer, long auditId) {
        GrcAudit audit = require(auditId);
        assertCanDownload(viewer, audit);
        try {
            byte[] bytes = excelWriter.write(assemble(audit, personLine(viewer), includeNotes(audit, viewer)));
            return new ExportFile(bytes, fileBase(audit) + "-report.xlsx", AuditReportExcelWriter.XLSX);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not export the audit report", e);
        }
    }

    private AuditReportData assemble(GrcAudit audit, String generatedBy, boolean includeWorkingNotes) {
        User lead = audit.getLeadAuditorUserId() == null
                ? null
                : userRepository.findById(audit.getLeadAuditorUserId()).orElse(null);
        User auditorSigner = audit.getAuditorSignedByUserId() == null
                ? null
                : userRepository.findById(audit.getAuditorSignedByUserId()).orElse(null);
        User adminSigner = audit.getAdminSignedByUserId() == null
                ? null
                : userRepository.findById(audit.getAdminSignedByUserId()).orElse(null);
        String auditee = audit.getAuditeeOrgUnitId() == null
                ? null
                : orgUnitRepository.findById(audit.getAuditeeOrgUnitId()).map(u -> u.getName()).orElse(null);

        List<AuditReportData.TeamMember> team = new ArrayList<>();
        boolean leadListed = false;
        for (GrcAuditTeamMember member : teamMemberRepository.findByAuditIdOrderByTeamRoleAscUserIdAsc(audit.getId())) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (lead != null && lead.getId().equals(member.getUserId())) {
                leadListed = true;
            }
            team.add(new AuditReportData.TeamMember(
                    user == null ? null : user.getFullName(),
                    user == null ? null : user.getEmail(),
                    AuditReportLabels.teamRole(member.getTeamRole())));
        }
        if (!leadListed && lead != null) {
            team.add(0, new AuditReportData.TeamMember(lead.getFullName(), lead.getEmail(), "Lead auditor"));
        }

        List<String> modules = scopeModuleRepository.findByAuditId(audit.getId()).stream()
                .map(row -> AuditReportLabels.scopeModule(row.getModuleCode()))
                .toList();
        int scopeNodes = scopeNodeRepository.findByAuditId(audit.getId()).size();

        List<GrcAuditChecklist> headers = checklistHeaderRepository.findByAuditIdOrderBySortOrderAscIdAsc(audit.getId());
        List<GrcAuditChecklistItem> items =
                checklistRepository.findByAuditIdOrderByChecklistIdAscSortOrderAscIdAsc(audit.getId());
        Map<Long, List<GrcAuditChecklistItem>> bySheet = new HashMap<>();
        Map<Long, String> clauseByItem = new HashMap<>();
        for (GrcAuditChecklistItem item : items) {
            bySheet.computeIfAbsent(item.getChecklistId(), id -> new ArrayList<>()).add(item);
            clauseByItem.put(item.getId(), item.getNodeCode());
        }
        List<AuditReportData.ChecklistSheet> sheets = new ArrayList<>();
        for (GrcAuditChecklist header : headers) {
            sheets.add(new AuditReportData.ChecklistSheet(
                    header.getName(), bySheet.getOrDefault(header.getId(), List.of())));
        }

        List<GrcAuditFinding> findingRows = findingRepository.findByAuditIdOrderByDisplayCodeAsc(audit.getId());
        Set<Long> capaIds = new HashSet<>();
        for (GrcAuditFinding finding : findingRows) {
            if (finding.getCapaId() != null) {
                capaIds.add(finding.getCapaId());
            }
        }
        Map<Long, Capa> capas = new HashMap<>();
        if (!capaIds.isEmpty()) {
            for (Capa capa : capaRepository.findAllById(capaIds)) {
                capas.put(capa.getId(), capa);
            }
        }
        List<AuditReportData.FindingLine> findings = new ArrayList<>();
        for (GrcAuditFinding finding : findingRows) {
            String clause = finding.getChecklistItemId() == null
                    ? finding.getControlRef()
                    : clauseByItem.getOrDefault(finding.getChecklistItemId(), finding.getControlRef());
            findings.add(new AuditReportData.FindingLine(finding, clause, capaLabel(finding.getCapaId(), capas)));
        }

        List<AuditReportData.DomainNarrative> narratives = new ArrayList<>();
        for (AuditDomainNarrativeResponse row : AuditDomainNarrativeJson.parse(audit.getDomainNarrativeJson())) {
            narratives.add(new AuditReportData.DomainNarrative(row.code(), row.name(), row.narrative()));
        }

        String classification = auditProperties.getReports().getClassificationFooter();
        return new AuditReportData(
                organisationName(),
                generatedBy,
                classification == null || classification.isBlank() ? "Confidential" : classification.trim(),
                Instant.now(),
                audit,
                lead == null ? null : lead.getFullName(),
                lead == null ? null : lead.getEmail(),
                auditee,
                auditorSigner == null ? null : personLine(auditorSigner),
                adminSigner == null ? null : personLine(adminSigner),
                team,
                modules,
                scopeNodes,
                sheets,
                findings,
                narratives,
                includeWorkingNotes);
    }

    private static String capaLabel(Long capaId, Map<Long, Capa> capas) {
        if (capaId == null) {
            return "-";
        }
        String code = AuditReportLabels.capaCode(capaId);
        Capa capa = capas.get(capaId);
        if (capa == null || capa.getStatus() == null) {
            return code;
        }
        return code + " (" + AuditReportLabels.capaStatus(capa.getStatus()) + ")";
    }

    private boolean includeNotes(GrcAudit audit, User viewer) {
        if (viewer == null || isAdmin(viewer)) {
            return true;
        }
        if (viewer.getId().equals(audit.getLeadAuditorUserId())) {
            return true;
        }
        if (teamMemberRepository.existsByAuditIdAndUserId(audit.getId(), viewer.getId())) {
            return true;
        }
        if (externalAccessRepository.existsByAuditIdAndUserIdAndStatus(
                audit.getId(), viewer.getId(), ExternalAccessStatus.ACTIVE)) {
            return true;
        }
        return audit.getStatus().notesVisibleToAdmin() || audit.isLocked();
    }

    private GrcAudit require(long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
    }

    void assertCanDownload(User viewer, GrcAudit audit) {
        if (isAdmin(viewer)) {
            return;
        }
        if (viewer.getId().equals(audit.getLeadAuditorUserId())) {
            return;
        }
        if (viewer.getRoles().contains(Role.INTERNAL_AUDITOR)) {
            return;
        }
        if (viewer.getRoles().contains(Role.EXTERNAL_AUDITOR)
                && externalAccessRepository.existsByAuditIdAndUserIdAndStatus(
                audit.getId(), viewer.getId(), ExternalAccessStatus.ACTIVE)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorised to download this report");
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
    }

    private String organisationName() {
        String tenantId = TenantContext.getTenantId();
        return tenantRepository.findByTenantId(tenantId).map(Tenant::getName).orElse(tenantId);
    }

    private static String personLine(User user) {
        if (user == null) {
            return "-";
        }
        String name = user.getFullName() == null ? "" : user.getFullName().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (name.isEmpty()) {
            return email.isEmpty() ? "-" : email;
        }
        if (email.isEmpty()) {
            return name;
        }
        return name + " (" + email + ")";
    }

    private static String fileBase(GrcAudit audit) {
        String id = audit.getDisplayId() == null || audit.getDisplayId().isBlank()
                ? "audit" : sanitizeFilename(audit.getDisplayId());
        String name = sanitizeFilename(audit.getName());
        String base = id + "-" + name;
        return base.length() > 80 ? base.substring(0, 80) : base;
    }

    private static String sanitizeFilename(String name) {
        String base = name == null ? "audit" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return base.isBlank() ? "audit" : base;
    }
}
