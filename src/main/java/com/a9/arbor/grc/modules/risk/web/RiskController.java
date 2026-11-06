package com.a9.arbor.grc.modules.risk.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.risk.domain.HeatMapScoreBasis;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.service.RiskLifecycleScheduler;
import com.a9.arbor.grc.modules.risk.service.RiskMatrixService;
import com.a9.arbor.grc.modules.risk.service.RiskService;
import com.a9.arbor.grc.modules.risk.web.dto.AcceptanceAdminDecideRequest;
import com.a9.arbor.grc.modules.risk.web.dto.AcceptanceOwnerDecideRequest;
import com.a9.arbor.grc.modules.risk.web.dto.AssignRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.OwnerReassignRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CancelRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.EligibleContributorResponse;
import com.a9.arbor.grc.modules.risk.web.dto.ChangeTreatmentRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CloseRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CompleteMilestoneRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskStageNoteRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CreateMilestoneRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.FinalRiskStatusRequest;
import com.a9.arbor.grc.modules.risk.web.dto.PostTreatmentRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskAcceptanceFormResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RejectValidationRequest;
import com.a9.arbor.grc.modules.risk.web.dto.ReopenRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskScoreHistoryResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskStageNoteResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskHeatMapResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskKpisResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskMatricesResponse;
import com.a9.arbor.grc.modules.risk.web.dto.UpdateRiskMatricesRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.a9.arbor.grc.modules.risk.web.dto.SubmitRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.UpdateRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.UpsertTreatmentPlanRequest;
import com.a9.arbor.grc.modules.risk.web.dto.ValidateApproveRequest;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.dashboard.service.ReportService;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.risk.service.RiskBulkImportService;
import com.a9.arbor.grc.modules.risk.service.RiskFormDraftService;
import com.a9.arbor.grc.modules.risk.web.dto.RiskFormDraftRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskFormDraftResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskBulkImportResultResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risks")
@RequiresModule({ModuleCode.ISMS, ModuleCode.ERM, ModuleCode.PRIVACY})
public class RiskController {

    private final RiskService riskService;
    private final RiskMatrixService riskMatrixService;
    private final RiskLifecycleScheduler riskLifecycleScheduler;
    private final RiskBulkImportService riskBulkImportService;
    private final ReportService reportService;
    private final UserService userService;
    private final RiskFormDraftService riskFormDraftService;

    public RiskController(RiskService riskService,
                          RiskMatrixService riskMatrixService,
                          RiskLifecycleScheduler riskLifecycleScheduler,
                          RiskBulkImportService riskBulkImportService,
                          ReportService reportService,
                          UserService userService,
                          RiskFormDraftService riskFormDraftService) {
        this.riskFormDraftService = riskFormDraftService;
        this.riskService = riskService;
        this.riskMatrixService = riskMatrixService;
        this.riskLifecycleScheduler = riskLifecycleScheduler;
        this.riskBulkImportService = riskBulkImportService;
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<RiskRecordResponse> list(Authentication authentication,
                                         @RequestParam(required = false) RiskType riskType,
                                         @RequestParam(required = false) RiskLevel probability,
                                         @RequestParam(required = false) RiskLevel impact,
                                         @RequestParam(required = false) RiskLevel residualRisk,
                                         @RequestParam(required = false) RiskLevel controlEffectiveness,
                                         @RequestParam(required = false) RiskLevel inherentRisk,
                                         @RequestParam(required = false) RiskLevel residualProbability,
                                         @RequestParam(required = false) RiskLevel residualImpact,
                                         @RequestParam(required = false) Boolean openOnly,
                                         @RequestParam(required = false) Long projectId,
                                         @RequestParam(required = false) RiskStatus status) {
        return riskService.list(currentUser(authentication), riskType,
                probability, impact, residualRisk, controlEffectiveness, inherentRisk,
                residualProbability, residualImpact, openOnly, projectId, status);
    }

    /** Interactive 5×5 risk heat map (J8 / §19.2). */
    @GetMapping("/heatmap")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RiskHeatMapResponse heatMap(Authentication authentication,
                                       @RequestParam RiskType riskType,
                                       @RequestParam(defaultValue = "INHERENT") HeatMapScoreBasis scoreBasis,
                                       @RequestParam(defaultValue = "true") boolean openOnly) {
        return riskService.heatMap(currentUser(authentication), riskType, scoreBasis, openOnly);
    }

    /** Open-risk band counts for the project picker. Does not hydrate register rows. */
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RiskKpisResponse kpis(Authentication authentication,
                                 @RequestParam(required = false) RiskType riskType) {
        return riskService.kpis(currentUser(authentication), riskType);
    }

    /**
     * Run Treat OVERDUE + Tolerate review jobs for this tenant (Path B3).
     * Optional {@code asOf=YYYY-MM-DD} for tests / backdated runs.
     */
    @PostMapping("/jobs/lifecycle-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = riskLifecycleScheduler.runCurrentTenant(day);
        return Map.of(
                "asOf", day.toString(),
                "overdueMarked", result.overdueMarked(),
                "reviewReminders", result.reviewReminders(),
                "toleranceReturned", result.toleranceReturned(),
                "assignmentChases", result.assignmentChases(),
                "ownerChases", result.ownerChases(),
                "overdueEscalations", result.overdueEscalations());
    }

    @GetMapping("/eligible-contributors")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public List<EligibleContributorResponse> eligibleContributors(@RequestParam Long orgUnitId) {
        return riskService.eligibleContributors(orgUnitId);
    }

    @GetMapping("/matrices")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RiskMatricesResponse getMatrices() {
        return riskMatrixService.getMatrices();
    }

    @PutMapping("/matrices")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskMatricesResponse updateMatrices(Authentication authentication,
                                               @Valid @RequestBody UpdateRiskMatricesRequest request) {
        return riskMatrixService.updateMatrices(adminUser(authentication), request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RiskRecordResponse get(Authentication authentication, @PathVariable long id) {
        return riskService.get(currentUser(authentication), id);
    }

    /** One IS or ERM risk — Excel (default) or PDF. Same roles as register export. */
    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> exportOne(Authentication authentication,
                                            @PathVariable long id,
                                            @RequestParam(defaultValue = "xlsx") String format) {
        User user = exporterUser(authentication);
        ExportFile file = reportService.exportSingleRisk(user, id, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse create(Authentication authentication,
                                     @Valid @RequestBody CreateRiskRequest request) {
        return riskService.create(adminUser(authentication), request);
    }

    /** Unfinished create forms for this project, started by the caller only. */
    @GetMapping("/drafts")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<RiskFormDraftResponse> listDrafts(Authentication authentication,
                                                  @RequestParam RiskType riskType,
                                                  @RequestParam Long projectId) {
        return riskFormDraftService.list(adminUser(authentication), riskType, projectId);
    }

    @GetMapping("/drafts/{draftId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskFormDraftResponse getDraft(Authentication authentication, @PathVariable long draftId) {
        return riskFormDraftService.get(adminUser(authentication), draftId);
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskFormDraftResponse createDraft(Authentication authentication,
                                             @RequestBody RiskFormDraftRequest request) {
        return riskFormDraftService.create(adminUser(authentication), request);
    }

    @PutMapping("/drafts/{draftId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskFormDraftResponse updateDraft(Authentication authentication,
                                             @PathVariable long draftId,
                                             @RequestBody RiskFormDraftRequest request) {
        return riskFormDraftService.update(adminUser(authentication), draftId, request);
    }

    @DeleteMapping("/drafts/{draftId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Void> deleteDraft(Authentication authentication, @PathVariable long draftId) {
        riskFormDraftService.delete(adminUser(authentication), draftId);
        return ResponseEntity.noContent().build();
    }

    /** Create the risk from a draft: normal required checks; draft attachments move to the risk. */
    @PostMapping("/drafts/{draftId}/create")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse createFromDraft(Authentication authentication,
                                              @PathVariable long draftId,
                                              @Valid @RequestBody CreateRiskRequest request) {
        return riskFormDraftService.createRisk(adminUser(authentication), draftId, request);
    }

    /** Download Arbor bulk-create template (IS or ERM). Includes Reference dropdown lists. */
    @GetMapping("/template.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> downloadBulkTemplate(Authentication authentication,
                                                       @RequestParam RiskType riskType,
                                                       @RequestParam(required = false) Long projectId) {
        var file = riskBulkImportService.downloadTemplate(adminUser(authentication), riskType, projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    /**
     * Bulk create from Arbor template. Commits valid rows; returns per-row success/error report.
     * Create-only: existing risks are kept. Pass projectId to add rows to that project.
     */
    @PostMapping(value = "/import.xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskBulkImportResultResponse importBulk(Authentication authentication,
                                                   @RequestParam RiskType riskType,
                                                   @RequestParam(required = false) Long projectId,
                                                   @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an Arbor risk Excel template");
        }
        try {
            return riskBulkImportService.importExcel(adminUser(authentication), riskType, file.getBytes(), projectId);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse update(Authentication authentication,
                                     @PathVariable long id,
                                     @Valid @RequestBody UpdateRiskRequest request) {
        return riskService.update(adminUser(authentication), id, request);
    }

    @PutMapping("/{id}/owner")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse reassignOwner(Authentication authentication,
                                            @PathVariable long id,
                                            @Valid @RequestBody OwnerReassignRequest request) {
        return riskService.reassignOwner(adminUser(authentication), id, request.userId());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse assign(Authentication authentication,
                                     @PathVariable long id,
                                     @Valid @RequestBody AssignRiskRequest request) {
        return riskService.assign(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse cancel(Authentication authentication,
                                     @PathVariable long id,
                                     @RequestBody(required = false) @Valid CancelRiskRequest request) {
        return riskService.cancel(adminUser(authentication), id,
                request != null ? request : new CancelRiskRequest(null));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CONTRIBUTOR')")
    public RiskRecordResponse submit(Authentication authentication,
                                     @PathVariable long id,
                                     @Valid @RequestBody SubmitRiskRequest request) {
        return riskService.submit(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/validate/start")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse startValidation(Authentication authentication, @PathVariable long id) {
        return riskService.startValidation(adminUser(authentication), id);
    }

    @PostMapping("/{id}/validate/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse rejectValidation(Authentication authentication,
                                               @PathVariable long id,
                                               @Valid @RequestBody RejectValidationRequest request) {
        return riskService.rejectValidation(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/validate/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse approveValidation(Authentication authentication,
                                                @PathVariable long id,
                                                @RequestBody(required = false) ValidateApproveRequest request) {
        return riskService.approveValidation(adminUser(authentication), id,
                request != null ? request : new ValidateApproveRequest(null, null));
    }

    @PostMapping("/{id}/treatment-option")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse changeTreatment(Authentication authentication,
                                              @PathVariable long id,
                                              @Valid @RequestBody ChangeTreatmentRequest request) {
        return riskService.changeTreatmentOption(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse approve(Authentication authentication, @PathVariable long id) {
        return riskService.approve(adminUser(authentication), id);
    }

    @PutMapping("/{id}/treatment-plan")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse upsertTreatmentPlan(Authentication authentication,
                                                  @PathVariable long id,
                                                  @Valid @RequestBody UpsertTreatmentPlanRequest request) {
        return riskService.upsertTreatmentPlan(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse close(Authentication authentication,
                                    @PathVariable long id,
                                    @RequestBody(required = false) CloseRiskRequest request) {
        return riskService.close(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/milestones")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RiskRecordResponse addMilestone(Authentication authentication,
                                           @PathVariable long id,
                                           @Valid @RequestBody CreateMilestoneRequest request) {
        return riskService.addMilestone(currentUser(authentication), id, request);
    }

    @PutMapping("/{id}/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RiskRecordResponse completeMilestone(Authentication authentication,
                                                @PathVariable long id,
                                                @PathVariable long milestoneId,
                                                @RequestBody(required = false) CompleteMilestoneRequest request) {
        return riskService.completeMilestone(currentUser(authentication), id, milestoneId,
                request != null ? request.progressNotes() : null);
    }

    @GetMapping("/acceptance-register")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public java.util.List<RiskAcceptanceFormResponse> acceptanceRegister(Authentication authentication) {
        return riskService.listAcceptanceRegister(currentUser(authentication));
    }

    @PostMapping("/{id}/post-treatment")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse postTreatment(Authentication authentication,
                                            @PathVariable long id,
                                            @Valid @RequestBody PostTreatmentRequest request) {
        return riskService.postTreatment(adminUser(authentication), id, request);
    }

    @PutMapping("/{id}/final-status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse finalStatus(Authentication authentication,
                                          @PathVariable long id,
                                          @Valid @RequestBody FinalRiskStatusRequest request) {
        return riskService.decideFinalStatus(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/acceptance/owner-decide")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER')")
    public RiskRecordResponse ownerDecide(Authentication authentication,
                                          @PathVariable long id,
                                          @Valid @RequestBody AcceptanceOwnerDecideRequest request) {
        return riskService.ownerDecideAcceptance(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/acceptance/admin-decide")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse adminDecide(Authentication authentication,
                                          @PathVariable long id,
                                          @Valid @RequestBody AcceptanceAdminDecideRequest request) {
        return riskService.adminDecideAcceptance(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse reopen(Authentication authentication,
                                     @PathVariable long id,
                                     @Valid @RequestBody ReopenRiskRequest request) {
        return riskService.reopen(adminUser(authentication), id, request);
    }

    @GetMapping("/{id}/score-history")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<RiskScoreHistoryResponse> scoreHistory(Authentication authentication, @PathVariable long id) {
        return riskService.listScoreHistory(currentUser(authentication), id);
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<RiskStageNoteResponse> listNotes(Authentication authentication, @PathVariable long id) {
        return riskService.listNotes(currentUser(authentication), id);
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RiskStageNoteResponse addNote(Authentication authentication,
                                         @PathVariable long id,
                                         @Valid @RequestBody CreateRiskStageNoteRequest request) {
        return riskService.addNote(currentUser(authentication), id, request);
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(Authentication authentication,
                           @PathVariable long id,
                           @PathVariable long noteId) {
        riskService.deleteNote(currentUser(authentication), id, noteId);
    }

    @PostMapping("/{id}/raise-capa")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public com.a9.arbor.grc.modules.capa.web.dto.CapaResponse raiseCapa(Authentication authentication,
                                                                        @PathVariable long id) {
        return riskService.raiseCapa(adminUser(authentication), id);
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }

    private User exporterUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        r == Role.OWNER || r == Role.ADMIN
                                || r == Role.INTERNAL_AUDITOR || r == Role.EXTERNAL_AUDITOR))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Export not permitted"));
    }
}
