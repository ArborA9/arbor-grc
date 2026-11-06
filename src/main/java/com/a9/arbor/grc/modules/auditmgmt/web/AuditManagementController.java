package com.a9.arbor.grc.modules.auditmgmt.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditAccessGrantService;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditAccessLogService;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditChecklistLibraryService;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditLifecycleScheduler;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditManagementService;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditPortalService;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditReportService;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AccessGrantResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AccessLogResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AccessViewLogResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditorMyAccessResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalEvidenceItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalGapItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalRiskItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditPortalSoaItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AddChecklistItemRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditChecklistItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditChecklistLibraryResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.ChecklistTemplateResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateAuditChecklistRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpsertAuditChecklistLibraryRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditDetailResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditFindingResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditSummaryResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateAccessGrantRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateAuditRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateFindingRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.ReturnAuditRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.RevokeAccessGrantRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateAuditScopeRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateAuditSummaryRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateChecklistItemRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateFindingRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.FindingRegisterItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateAuditEngagementRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateDomainNarrativesRequest;
import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import org.springframework.web.bind.annotation.RequestParam;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.export.ExportFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audits")
@RequiresModule(ModuleCode.AUDIT)
public class AuditManagementController {

    private final AuditManagementService auditManagementService;
    private final AuditPortalService auditPortalService;
    private final AuditReportService auditReportService;
    private final UserService userService;
    private final AuditLifecycleScheduler auditLifecycleScheduler;
    private final AuditAccessGrantService accessGrantService;
    private final AuditAccessLogService accessLogService;
    private final AuditChecklistLibraryService checklistLibraryService;

    public AuditManagementController(AuditManagementService auditManagementService,
                                     AuditPortalService auditPortalService,
                                     AuditReportService auditReportService,
                                     UserService userService,
                                     AuditLifecycleScheduler auditLifecycleScheduler,
                                     AuditAccessGrantService accessGrantService,
                                     AuditAccessLogService accessLogService,
                                     AuditChecklistLibraryService checklistLibraryService) {
        this.auditManagementService = auditManagementService;
        this.auditPortalService = auditPortalService;
        this.auditReportService = auditReportService;
        this.userService = userService;
        this.auditLifecycleScheduler = auditLifecycleScheduler;
        this.accessGrantService = accessGrantService;
        this.accessLogService = accessLogService;
        this.checklistLibraryService = checklistLibraryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public List<AuditSummaryResponse> list(Authentication authentication) {
        return auditManagementService.list(current(authentication));
    }

    /** Union of modules on the caller's active access grants (Auditor console nav). */
    @GetMapping("/my-access")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public AuditorMyAccessResponse myAccess(Authentication authentication) {
        var user = current(authentication);
        var modules = new ArrayList<>(accessGrantService.grantedModulesForUser(user));
        modules.sort(Enum::compareTo);
        return new AuditorMyAccessResponse(modules, accessGrantService.activeGrantResponsesForUser(user));
    }

    @GetMapping("/checklist-templates")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public List<ChecklistTemplateResponse> checklistTemplates(Authentication authentication) {
        current(authentication);
        return auditManagementService.listTemplates();
    }

    @GetMapping("/checklist-library")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public List<AuditChecklistLibraryResponse> listChecklistLibrary(Authentication authentication) {
        return checklistLibraryService.list(current(authentication));
    }

    @GetMapping("/checklist-library/template.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public ResponseEntity<byte[]> checklistLibraryTemplate() {
        var file = checklistLibraryService.blankTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/checklist-library/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public AuditChecklistLibraryResponse getChecklistLibrary(Authentication authentication, @PathVariable long id) {
        return checklistLibraryService.get(current(authentication), id);
    }

    @PostMapping("/checklist-library")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuditChecklistLibraryResponse createChecklistLibrary(Authentication authentication,
                                                                @Valid @RequestBody UpsertAuditChecklistLibraryRequest request) {
        return checklistLibraryService.create(current(authentication), request);
    }

    @PutMapping("/checklist-library/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuditChecklistLibraryResponse saveChecklistLibrary(Authentication authentication,
                                                              @PathVariable long id,
                                                              @Valid @RequestBody UpsertAuditChecklistLibraryRequest request) {
        return checklistLibraryService.save(current(authentication), id, request);
    }

    @DeleteMapping("/checklist-library/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deleteChecklistLibrary(Authentication authentication, @PathVariable long id) {
        checklistLibraryService.delete(current(authentication), id);
    }

    @PostMapping(value = "/checklist-library/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuditChecklistLibraryResponse uploadChecklistLibrary(Authentication authentication,
                                                                @PathVariable long id,
                                                                @RequestParam("file") MultipartFile file) {
        return checklistLibraryService.upload(current(authentication), id, file);
    }

    @GetMapping("/grants")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER')")
    public List<AccessGrantResponse> listAllGrants(Authentication authentication) {
        current(authentication);
        return accessGrantService.listAll();
    }

    @GetMapping("/access-log")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER')")
    public List<AccessViewLogResponse> listAccessLog(Authentication authentication) {
        current(authentication);
        return accessLogService.listAllRecent();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public AuditDetailResponse get(Authentication authentication, @PathVariable long id) {
        return auditManagementService.get(current(authentication), id);
    }

    /** Engagement-scoped evidence for internal team (same filter as external portal). */
    @GetMapping("/{id}/scoped-evidence")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public List<AuditPortalEvidenceItemResponse> scopedEvidence(Authentication authentication,
                                                                @PathVariable long id) {
        return auditPortalService.listEvidence(current(authentication), id);
    }

    @GetMapping("/{id}/scoped-gap")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public List<AuditPortalGapItemResponse> scopedGap(Authentication authentication, @PathVariable long id) {
        return auditPortalService.listGapItems(current(authentication), id);
    }

    @GetMapping("/{id}/scoped-soa")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public List<AuditPortalSoaItemResponse> scopedSoa(Authentication authentication, @PathVariable long id) {
        return auditPortalService.listSoaItems(current(authentication), id);
    }

    @GetMapping("/{id}/scoped-risks")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER')")
    public List<AuditPortalRiskItemResponse> scopedRisks(Authentication authentication, @PathVariable long id) {
        return auditPortalService.listRisks(current(authentication), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuditDetailResponse create(Authentication authentication, @Valid @RequestBody CreateAuditRequest request) {
        return auditManagementService.create(current(authentication), request);
    }

    @GetMapping("/{id}/grants")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER')")
    public List<AccessGrantResponse> listGrants(Authentication authentication, @PathVariable long id) {
        auditManagementService.get(current(authentication), id);
        return accessGrantService.listForAudit(id);
    }

    @PostMapping("/{id}/grants")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AccessGrantResponse createGrant(Authentication authentication,
                                           @PathVariable long id,
                                           @Valid @RequestBody CreateAccessGrantRequest request) {
        return accessGrantService.create(current(authentication), id, request);
    }

    @PostMapping("/{id}/grants/{grantId}/revoke")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AccessGrantResponse revokeGrant(Authentication authentication,
                                           @PathVariable long id,
                                           @PathVariable long grantId,
                                           @Valid @RequestBody RevokeAccessGrantRequest request) {
        current(authentication);
        auditManagementService.get(current(authentication), id);
        return accessGrantService.revoke(current(authentication), grantId, request.reason());
    }

    @GetMapping("/{id}/grants/{grantId}/log")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER')")
    public List<AccessLogResponse> grantLog(Authentication authentication,
                                            @PathVariable long id,
                                            @PathVariable long grantId) {
        auditManagementService.get(current(authentication), id);
        return accessLogService.listForGrant(grantId).stream().map(AccessLogResponse::from).toList();
    }

    @GetMapping("/findings/register")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','GLOBAL_READER')")
    public List<FindingRegisterItemResponse> listOpenFindings(Authentication authentication,
                                                              @RequestParam(required = false) FindingSeverity severity) {
        return auditManagementService.listOpenFindings(current(authentication), severity);
    }

    @PutMapping("/{id}/engagement")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuditDetailResponse updateEngagement(Authentication authentication,
                                              @PathVariable long id,
                                              @Valid @RequestBody UpdateAuditEngagementRequest request) {
        return auditManagementService.updateEngagement(current(authentication), id, request);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse start(Authentication authentication, @PathVariable long id) {
        return auditManagementService.start(current(authentication), id);
    }

    @PostMapping("/{id}/sync-gap-scores")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse syncGapScores(Authentication authentication, @PathVariable long id) {
        return auditManagementService.syncGapScores(current(authentication), id);
    }

    @PutMapping("/{id}/domain-narratives")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse updateDomainNarratives(Authentication authentication,
                                                      @PathVariable long id,
                                                      @Valid @RequestBody UpdateDomainNarrativesRequest request) {
        return auditManagementService.updateDomainNarratives(current(authentication), id, request);
    }

    @PutMapping("/{id}/checklist/{itemId}")
    @PreAuthorize("hasAnyRole('INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditChecklistItemResponse updateChecklist(Authentication authentication,
                                                      @PathVariable long id,
                                                      @PathVariable long itemId,
                                                      @RequestBody UpdateChecklistItemRequest request) {
        return auditManagementService.updateChecklist(current(authentication), id, itemId, request);
    }

    @DeleteMapping("/{id}/checklist/{itemId}")
    @PreAuthorize("hasAnyRole('INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse deleteChecklistItem(Authentication authentication,
                                                   @PathVariable long id,
                                                   @PathVariable long itemId) {
        return auditManagementService.deleteChecklistItem(current(authentication), id, itemId);
    }

    @PostMapping("/{id}/checklists")
    @PreAuthorize("hasAnyRole('INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse addChecklist(Authentication authentication,
                                            @PathVariable long id,
                                            @RequestBody CreateAuditChecklistRequest request) {
        return auditManagementService.addChecklist(current(authentication), id, request);
    }

    @PostMapping(value = "/{id}/checklists/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse uploadNewChecklist(Authentication authentication,
                                                  @PathVariable long id,
                                                  @RequestParam("name") String name,
                                                  @RequestParam("file") MultipartFile file) {
        return auditManagementService.uploadNewChecklist(current(authentication), id, name, file);
    }

    @PostMapping(value = "/{id}/checklists/{checklistId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse uploadChecklist(Authentication authentication,
                                               @PathVariable long id,
                                               @PathVariable long checklistId,
                                               @RequestParam("file") MultipartFile file) {
        return auditManagementService.uploadChecklist(current(authentication), id, checklistId, file);
    }

    @DeleteMapping("/{id}/checklists/{checklistId}")
    @PreAuthorize("hasAnyRole('INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse deleteChecklist(Authentication authentication,
                                               @PathVariable long id,
                                               @PathVariable long checklistId) {
        return auditManagementService.deleteChecklist(current(authentication), id, checklistId);
    }

    @PostMapping("/{id}/checklists/{checklistId}/items")
    @PreAuthorize("hasAnyRole('INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditChecklistItemResponse addChecklistItem(Authentication authentication,
                                                       @PathVariable long id,
                                                       @PathVariable long checklistId,
                                                       @Valid @RequestBody AddChecklistItemRequest request) {
        return auditManagementService.addChecklistItem(current(authentication), id, checklistId, request);
    }

    @PostMapping("/{id}/findings")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditFindingResponse addFinding(Authentication authentication,
                                           @PathVariable long id,
                                           @Valid @RequestBody CreateFindingRequest request) {
        return auditManagementService.addFinding(current(authentication), id, request);
    }

    @PutMapping("/{id}/findings/{findingId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditFindingResponse updateFinding(Authentication authentication,
                                              @PathVariable long id,
                                              @PathVariable long findingId,
                                              @RequestBody UpdateFindingRequest request) {
        return auditManagementService.updateFinding(current(authentication), id, findingId, request);
    }

    @DeleteMapping("/{id}/findings/{findingId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public void deleteFinding(Authentication authentication,
                              @PathVariable long id,
                              @PathVariable long findingId) {
        auditManagementService.deleteFinding(current(authentication), id, findingId);
    }

    @PutMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse updateSummary(Authentication authentication,
                                             @PathVariable long id,
                                             @RequestBody UpdateAuditSummaryRequest request) {
        return auditManagementService.updateSummary(current(authentication), id, request);
    }

    @PutMapping("/{id}/scope")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse updateScope(Authentication authentication,
                                           @PathVariable long id,
                                           @RequestBody UpdateAuditScopeRequest request) {
        return auditManagementService.updateScope(current(authentication), id, request);
    }

    @PostMapping("/{id}/auditor-sign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse auditorSign(Authentication authentication, @PathVariable long id) {
        return auditManagementService.auditorSign(current(authentication), id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AuditDetailResponse close(Authentication authentication, @PathVariable long id) {
        return auditManagementService.close(current(authentication), id);
    }

    @PostMapping("/{id}/admin-sign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuditDetailResponse adminSign(Authentication authentication, @PathVariable long id) {
        return auditManagementService.adminSign(current(authentication), id);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AuditDetailResponse returnAudit(Authentication authentication,
                                           @PathVariable long id,
                                           @Valid @RequestBody ReturnAuditRequest request) {
        return auditManagementService.returnToAuditor(current(authentication), id, request);
    }

    /** Audit project PDF. Owner, Admin, internal auditor, lead, or an external auditor on this audit. */
    @GetMapping("/{id}/report.pdf")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public ResponseEntity<byte[]> downloadReport(Authentication authentication, @PathVariable long id) {
        return exportFile(auditReportService.download(current(authentication), id));
    }

    /** Audit project Excel. Same audience as the PDF. */
    @GetMapping("/{id}/report.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public ResponseEntity<byte[]> downloadReportExcel(Authentication authentication, @PathVariable long id) {
        return exportFile(auditReportService.excel(current(authentication), id));
    }

    private static ResponseEntity<byte[]> exportFile(ExportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping("/jobs/lifecycle-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        current(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = auditLifecycleScheduler.runCurrentTenant(day);
        return Map.of(
                "asOf", day.toString(),
                "startDate", result.startDate(),
                "overdue", result.overdue(),
                "reportPending", result.reportPending());
    }

    private User current(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
