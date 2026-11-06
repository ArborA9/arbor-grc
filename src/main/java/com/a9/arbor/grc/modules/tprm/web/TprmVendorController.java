package com.a9.arbor.grc.modules.tprm.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.a9.arbor.grc.modules.tprm.service.TprmAssessmentService;
import com.a9.arbor.grc.modules.tprm.service.TprmQuestionnaireService;
import com.a9.arbor.grc.modules.tprm.service.TprmReportService;
import com.a9.arbor.grc.modules.tprm.service.TprmVendorService;
import com.a9.arbor.grc.modules.tprm.web.dto.AddCollaboratorRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.AssessmentResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.CreateAssessmentRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.CreateTprmProjectRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.FindingResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.QuestionnaireResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.ReviewAssessmentRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.TprmKpiResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.TprmLookupsResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.UpdateFindingRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.UpdateTprmProjectRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.UpsertQuestionnaireRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.UpsertVendorRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.VendorResponse;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tprm")
@RequiresModule(ModuleCode.ERM)
public class TprmVendorController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','EXTERNAL_AUDITOR','INTERNAL_AUDITOR')";

    private final TprmVendorService vendorService;
    private final TprmAssessmentService assessmentService;
    private final TprmQuestionnaireService questionnaireService;
    private final TprmReportService reportService;
    private final UserService userService;

    public TprmVendorController(TprmVendorService vendorService,
                                 TprmAssessmentService assessmentService,
                                 TprmQuestionnaireService questionnaireService,
                                 TprmReportService reportService,
                                 UserService userService) {
        this.vendorService = vendorService;
        this.assessmentService = assessmentService;
        this.questionnaireService = questionnaireService;
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping("/lookups")
    @PreAuthorize(READ_ROLES)
    public TprmLookupsResponse lookups() {
        return vendorService.lookups();
    }

    @GetMapping("/kpis")
    @PreAuthorize(READ_ROLES)
    public TprmKpiResponse kpis() {
        return vendorService.kpis();
    }

    @GetMapping("/vendors")
    @PreAuthorize(READ_ROLES)
    public List<VendorResponse> list() {
        return vendorService.list();
    }

    @GetMapping("/vendors/{id}")
    @PreAuthorize(READ_ROLES)
    public VendorResponse get(@PathVariable long id) {
        return vendorService.get(id);
    }

    @PostMapping("/vendors")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public VendorResponse create(Authentication authentication, @Valid @RequestBody UpsertVendorRequest request) {
        return vendorService.create(currentUser(authentication), request);
    }

    @PutMapping("/vendors/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public VendorResponse update(Authentication authentication, @PathVariable long id,
                                 @Valid @RequestBody UpsertVendorRequest request) {
        VendorResponse updated = vendorService.update(currentUser(authentication), id, request);
        assessmentService.resyncDraftQuestionnaire(id);
        return updated;
    }

    @PostMapping("/vendors/{vendorId}/questionnaire")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse generate(Authentication authentication, @PathVariable long vendorId) {
        return assessmentService.generate(currentUser(authentication), vendorId);
    }

    @PostMapping("/vendors/{vendorId}/assessments")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse issue(Authentication authentication, @PathVariable long vendorId,
                                    @Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.issue(currentUser(authentication), vendorId, request);
    }

    @GetMapping("/assessments")
    @PreAuthorize(READ_ROLES)
    public List<AssessmentResponse> listAssessments(@RequestParam(required = false) Long vendorId) {
        return assessmentService.list(vendorId);
    }

    @GetMapping("/assessments/{id}")
    @PreAuthorize(READ_ROLES)
    public AssessmentResponse getAssessment(@PathVariable long id) {
        return assessmentService.get(id);
    }

    @GetMapping("/assessments/{id}/questionnaire.xlsx")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<byte[]> downloadExcel(@PathVariable long id) {
        var file = assessmentService.downloadExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping(value = "/assessments/{id}/questionnaire.xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse uploadExcel(Authentication authentication, @PathVariable long id,
                                          @RequestParam("file") MultipartFile file) {
        return assessmentService.uploadExcel(currentUser(authentication), id, file);
    }

    @PostMapping("/assessments/{id}/review")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse review(Authentication authentication, @PathVariable long id,
                                     @Valid @RequestBody ReviewAssessmentRequest request) {
        return assessmentService.review(currentUser(authentication), id, request);
    }

    @PostMapping("/assessments/{id}/raise-erm")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskRecordResponse raiseErm(Authentication authentication, @PathVariable long id) {
        return assessmentService.raiseErm(currentUser(authentication), id);
    }

    @GetMapping("/findings")
    @PreAuthorize(READ_ROLES)
    public List<FindingResponse> findings(@RequestParam(required = false) Long vendorId) {
        return assessmentService.listFindings(vendorId);
    }

    @PutMapping("/findings/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public FindingResponse updateFinding(Authentication authentication, @PathVariable long id,
                                         @Valid @RequestBody UpdateFindingRequest request) {
        return assessmentService.updateFinding(currentUser(authentication), id, request);
    }

    @GetMapping("/questionnaires")
    @PreAuthorize(READ_ROLES)
    public List<QuestionnaireResponse> questionnaires(Authentication authentication) {
        return questionnaireService.list(currentUser(authentication));
    }

    @GetMapping("/questionnaires/sample.xlsx")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<byte[]> questionnaireLibrarySample() {
        var file = questionnaireService.librarySample();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/questionnaires/{id}")
    @PreAuthorize(READ_ROLES)
    public QuestionnaireResponse getQuestionnaire(Authentication authentication, @PathVariable long id) {
        return questionnaireService.get(currentUser(authentication), id);
    }

    @PostMapping("/questionnaires")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public QuestionnaireResponse createQuestionnaire(Authentication authentication,
                                                       @Valid @RequestBody UpsertQuestionnaireRequest request) {
        return questionnaireService.create(currentUser(authentication), request);
    }

    @PutMapping("/questionnaires/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public QuestionnaireResponse saveQuestionnaire(Authentication authentication, @PathVariable long id,
                                                     @Valid @RequestBody UpsertQuestionnaireRequest request) {
        return questionnaireService.save(currentUser(authentication), id, request);
    }

    @GetMapping("/questionnaires/{id}/sample.xlsx")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<byte[]> questionnaireSample(@PathVariable long id) {
        var file = questionnaireService.sample(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping(value = "/questionnaires/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public QuestionnaireResponse uploadQuestionnaire(Authentication authentication, @PathVariable long id,
                                                    @RequestParam("file") MultipartFile file) {
        return questionnaireService.upload(currentUser(authentication), id, file);
    }

    @GetMapping("/projects")
    @PreAuthorize(READ_ROLES)
    public List<AssessmentResponse> projects() {
        return assessmentService.list(null);
    }

    @GetMapping("/projects/{id}")
    @PreAuthorize(READ_ROLES)
    public AssessmentResponse getProject(@PathVariable long id) {
        return assessmentService.get(id);
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse createProject(Authentication authentication,
                                             @Valid @RequestBody CreateTprmProjectRequest request) {
        return assessmentService.createProject(currentUser(authentication), request);
    }

    @PutMapping("/projects/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse updateProject(Authentication authentication, @PathVariable long id,
                                            @Valid @RequestBody UpdateTprmProjectRequest request) {
        return assessmentService.updateProject(currentUser(authentication), id, request);
    }

    @PostMapping("/projects/{id}/issue")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse issueProject(Authentication authentication, @PathVariable long id,
                                             @Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.issueProject(currentUser(authentication), id, request);
    }

    @PostMapping("/projects/{id}/collaborators")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssessmentResponse addCollaborator(Authentication authentication, @PathVariable long id,
                                              @Valid @RequestBody AddCollaboratorRequest request) {
        return assessmentService.addCollaborator(currentUser(authentication), id, request);
    }

    @GetMapping("/projects/{id}/export.xlsx")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<byte[]> exportProjectXlsx(Authentication authentication, @PathVariable long id) {
        var file = reportService.project(currentUser(authentication), "Organization", id, false);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/projects/{id}/export.pdf")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<byte[]> exportProjectPdf(Authentication authentication, @PathVariable long id) {
        var file = reportService.project(currentUser(authentication), "Organization", id, true);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
