package com.a9.arbor.grc.modules.gap.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.gap.service.GapLifecycleScheduler;
import com.a9.arbor.grc.modules.gap.service.GapAssessmentService;
import com.a9.arbor.grc.modules.gap.web.dto.AssignGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.ControlAssessmentStatusResponse;
import com.a9.arbor.grc.modules.gap.web.dto.CreateGapAssessmentRequest;
import com.a9.arbor.grc.modules.gap.web.dto.FinalizeGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.GapAssessmentDetailResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapAssessmentSummaryResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapItemLocatorResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapItemResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapItemSubmissionResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapQuestionAnswerResponse;
import com.a9.arbor.grc.modules.gap.web.dto.SendBackGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.SendBackGapItemsRequest;
import com.a9.arbor.grc.modules.gap.web.dto.SendGapItemsRequest;
import com.a9.arbor.grc.modules.gap.web.dto.SetGapFlagRequest;
import com.a9.arbor.grc.modules.gap.web.dto.UpdateGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.UpsertGapAnswersRequest;
import com.a9.arbor.grc.modules.capa.web.dto.CapaResponse;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gap-assessments")
@RequiresModule({
        ModuleCode.ISMS, ModuleCode.BCMS, ModuleCode.QMS, ModuleCode.OHSMS, ModuleCode.SAMA,
        ModuleCode.ITSM, ModuleCode.AIMS, ModuleCode.PRIVACY
})
public class GapAssessmentController {

    private final GapAssessmentService gapAssessmentService;
    private final UserService userService;
    private final com.a9.arbor.grc.modules.risk.service.RiskService riskService;
    private final GapLifecycleScheduler gapLifecycleScheduler;

    public GapAssessmentController(GapAssessmentService gapAssessmentService,
                                   UserService userService,
                                   com.a9.arbor.grc.modules.risk.service.RiskService riskService,
                                   GapLifecycleScheduler gapLifecycleScheduler) {
        this.gapAssessmentService = gapAssessmentService;
        this.userService = userService;
        this.riskService = riskService;
        this.gapLifecycleScheduler = gapLifecycleScheduler;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<GapAssessmentSummaryResponse> list(Authentication authentication,
                                                   @RequestParam(required = false) Long projectId) {
        return gapAssessmentService.list(currentUser(authentication), projectId);
    }

    /** Notification click: resolve a Gap control id to the assessment to open. */
    @GetMapping("/by-item/{itemId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public GapItemLocatorResponse locateItem(Authentication authentication, @PathVariable long itemId) {
        return gapAssessmentService.locateItem(currentUser(authentication), itemId);
    }

    /** Doc 02 R-09: Gap status for framework + control (risk identification). */
    @GetMapping("/control-assessment-status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public ControlAssessmentStatusResponse controlAssessmentStatus(
            @RequestParam long frameworkId,
            @RequestParam long frameworkNodeId) {
        return gapAssessmentService.latestControlAssessmentStatus(frameworkId, frameworkNodeId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public GapAssessmentDetailResponse get(Authentication authentication, @PathVariable long id) {
        return gapAssessmentService.get(currentUser(authentication), id);
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public GapAssessmentSummaryResponse summary(Authentication authentication, @PathVariable long id) {
        return gapAssessmentService.header(currentUser(authentication), id);
    }

    @GetMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public GapItemResponse getItem(Authentication authentication,
                                   @PathVariable long id,
                                   @PathVariable long itemId) {
        return gapAssessmentService.getItem(currentUser(authentication), id, itemId);
    }

    /** Excel export: IN_PROGRESS or COMPLETED. Contributor / Global Reader cannot export. */
    @GetMapping("/{id}/export.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication, @PathVariable long id) {
        var file = gapAssessmentService.exportExcel(exporterUser(authentication), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    /** PDF export: IN_PROGRESS or COMPLETED (§19.3). */
    @GetMapping("/{id}/export.pdf")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<byte[]> exportPdf(Authentication authentication, @PathVariable long id) {
        var file = gapAssessmentService.exportPdf(exporterUser(authentication), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    /** Raise an IS Risk from a Gap control (pre-populates framework / control / observed gap). */
    @PostMapping("/{id}/items/{itemId}/raise-risk")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse raiseRisk(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long itemId,
            @RequestBody(required = false) com.a9.arbor.grc.modules.risk.web.dto.CreateRiskRequest body) {
        gapAssessmentService.assertContributorSubmittedForRaise(id, itemId);
        return riskService.raiseFromGap(adminUser(authentication), id, itemId, body);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public GapAssessmentDetailResponse create(Authentication authentication,
                                              @Valid @RequestBody CreateGapAssessmentRequest request) {
        return gapAssessmentService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public GapItemResponse updateItem(Authentication authentication,
                                      @PathVariable long id,
                                      @PathVariable long itemId,
                                      @Valid @RequestBody UpdateGapItemRequest request) {
        return gapAssessmentService.updateItem(editor(authentication), id, itemId, request);
    }

    @PostMapping("/{id}/items/{itemId}/assign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public GapItemResponse assignItem(Authentication authentication,
                                      @PathVariable long id,
                                      @PathVariable long itemId,
                                      @Valid @RequestBody AssignGapItemRequest request) {
        return gapAssessmentService.assignItem(adminUser(authentication), id, itemId, request);
    }

    @PostMapping("/{id}/items/{itemId}/finalize")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public GapItemResponse finalizeItem(Authentication authentication,
                                        @PathVariable long id,
                                        @PathVariable long itemId,
                                        @Valid @RequestBody FinalizeGapItemRequest request) {
        return gapAssessmentService.finalizeItem(adminUser(authentication), id, itemId, request);
    }

    @PostMapping("/{id}/items/send")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<GapItemResponse> sendItems(Authentication authentication,
                                           @PathVariable long id,
                                           @Valid @RequestBody SendGapItemsRequest request) {
        return gapAssessmentService.sendItems(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/items/send-back")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<GapItemResponse> sendBackItems(Authentication authentication,
                                              @PathVariable long id,
                                              @Valid @RequestBody SendBackGapItemsRequest request) {
        return gapAssessmentService.sendBackItems(adminUser(authentication), id, request);
    }

    @PutMapping("/{id}/items/{itemId}/flag")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public GapItemResponse setFlag(Authentication authentication,
                                   @PathVariable long id,
                                   @PathVariable long itemId,
                                   @Valid @RequestBody SetGapFlagRequest request) {
        return gapAssessmentService.setFlag(adminUser(authentication), id, itemId, request);
    }

    @PostMapping("/{id}/items/{itemId}/send-back")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public GapItemResponse sendBackItem(Authentication authentication,
                                        @PathVariable long id,
                                        @PathVariable long itemId,
                                        @Valid @RequestBody SendBackGapItemRequest request) {
        return gapAssessmentService.sendBackItem(adminUser(authentication), id, itemId, request);
    }

    @PostMapping("/{id}/items/{itemId}/raise-capa")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CapaResponse raiseCapa(Authentication authentication,
                                  @PathVariable long id,
                                  @PathVariable long itemId) {
        return gapAssessmentService.raiseCapa(adminUser(authentication), id, itemId);
    }

    @GetMapping("/{id}/items/{itemId}/submissions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER')")
    public List<GapItemSubmissionResponse> listSubmissions(@PathVariable long id, @PathVariable long itemId) {
        return gapAssessmentService.listSubmissions(id, itemId);
    }

    @GetMapping("/{id}/items/{itemId}/questions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER')")
    public List<com.a9.arbor.grc.catalog.web.dto.FrameworkQuestionResponse> listQuestions(
            Authentication authentication,
            @PathVariable long id, @PathVariable long itemId) {
        return gapAssessmentService.listQuestionsForItem(currentUser(authentication), id, itemId);
    }

    @GetMapping("/{id}/items/{itemId}/answers")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER')")
    public List<GapQuestionAnswerResponse> listAnswers(Authentication authentication,
                                                       @PathVariable long id, @PathVariable long itemId) {
        return gapAssessmentService.listAnswers(currentUser(authentication), id, itemId);
    }

    @PutMapping("/{id}/items/{itemId}/answers")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public List<GapQuestionAnswerResponse> upsertAnswers(Authentication authentication,
                                                         @PathVariable long id,
                                                         @PathVariable long itemId,
                                                         @Valid @RequestBody UpsertGapAnswersRequest request) {
        return gapAssessmentService.upsertAnswers(editor(authentication), id, itemId, request);
    }

    @GetMapping("/{id}/items/{itemId}/custom-questions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER')")
    public List<com.a9.arbor.grc.modules.gap.web.dto.GapCustomQuestionResponse> listCustomQuestions(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long itemId) {
        return gapAssessmentService.listCustomQuestions(currentUser(authentication), id, itemId);
    }

    @PostMapping("/{id}/items/{itemId}/custom-questions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public com.a9.arbor.grc.modules.gap.web.dto.GapCustomQuestionResponse addCustomQuestion(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long itemId,
            @Valid @RequestBody com.a9.arbor.grc.modules.gap.web.dto.CreateGapCustomQuestionRequest request) {
        return gapAssessmentService.addCustomQuestion(adminUser(authentication), id, itemId, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public GapAssessmentDetailResponse complete(Authentication authentication, @PathVariable long id) {
        return gapAssessmentService.complete(adminUser(authentication), id);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public GapAssessmentDetailResponse reopen(Authentication authentication, @PathVariable long id) {
        return gapAssessmentService.reopen(adminUser(authentication), id);
    }

    @PostMapping("/jobs/lifecycle-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = gapLifecycleScheduler.runCurrentTenant(day);
        return Map.of("asOf", day.toString(), "notResponded", result.notResponded());
    }

    private User editor(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        r == Role.OWNER || r == Role.ADMIN || r == Role.CONTRIBUTOR))
                .filter(u -> u.getRoles().stream().noneMatch(r -> r == Role.VENDOR))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No gap access"));
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }

    private User exporterUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Export not permitted"));
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
