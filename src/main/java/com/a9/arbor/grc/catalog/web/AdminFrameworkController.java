package com.a9.arbor.grc.catalog.web;

import com.a9.arbor.grc.catalog.service.FrameworkBundleImportService;
import com.a9.arbor.grc.catalog.service.FrameworkContentEditService;
import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.catalog.service.FrameworkQuestionService;
import com.a9.arbor.grc.catalog.service.RiskLibraryService;
import com.a9.arbor.grc.catalog.web.dto.CreateFrameworkNodeRequest;
import com.a9.arbor.grc.catalog.web.dto.FrameworkBundleImportResultResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkBundleStatusResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkImportResultResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkQuestionImportResult;
import com.a9.arbor.grc.catalog.web.dto.FrameworkQuestionResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkTenantAssignmentResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkTypeResponse;
import com.a9.arbor.grc.catalog.web.dto.RenameSheetTabRequest;
import com.a9.arbor.grc.catalog.web.dto.ReorderFrameworkNodesRequest;
import com.a9.arbor.grc.catalog.web.dto.RiskTemplateImportResult;
import com.a9.arbor.grc.catalog.web.dto.RiskTemplateResponse;
import com.a9.arbor.grc.catalog.web.dto.UpdateFrameworkNodeRequest;
import com.a9.arbor.grc.catalog.web.dto.UpdateFrameworkTenantsRequest;
import jakarta.validation.Valid;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/frameworks")
public class AdminFrameworkController {

    private final FrameworkLibraryService frameworkLibraryService;
    private final FrameworkBundleImportService frameworkBundleImportService;
    private final FrameworkQuestionService frameworkQuestionService;
    private final RiskLibraryService riskLibraryService;
    private final FrameworkContentEditService frameworkContentEditService;

    public AdminFrameworkController(FrameworkLibraryService frameworkLibraryService,
                                    FrameworkBundleImportService frameworkBundleImportService,
                                    FrameworkQuestionService frameworkQuestionService,
                                    RiskLibraryService riskLibraryService,
                                    FrameworkContentEditService frameworkContentEditService) {
        this.frameworkLibraryService = frameworkLibraryService;
        this.frameworkBundleImportService = frameworkBundleImportService;
        this.frameworkQuestionService = frameworkQuestionService;
        this.riskLibraryService = riskLibraryService;
        this.frameworkContentEditService = frameworkContentEditService;
    }

    @GetMapping("/types")
    public List<FrameworkTypeResponse> types() {
        return frameworkLibraryService.listTypes();
    }

    @GetMapping("/types/{code}/sample")
    public ResponseEntity<byte[]> sample(@PathVariable String code) {
        byte[] bytes = frameworkLibraryService.sampleExcel(code);
        String filename = frameworkLibraryService.sampleFilename(code);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(filename))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping
    public List<FrameworkSummaryResponse> list() {
        return frameworkLibraryService.listAll();
    }

    @GetMapping("/{id:\\d+}")
    public FrameworkDetailResponse detail(@PathVariable Long id) {
        return frameworkLibraryService.getDetail(id);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FrameworkImportResultResponse importExcel(
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String version,
            @RequestParam(required = false, defaultValue = "false") boolean dryRun,
            @RequestParam("file") MultipartFile file) {
        return frameworkLibraryService.importExcel(typeCode, version, file, dryRun);
    }

    @PostMapping("/{id}/validate")
    public FrameworkSummaryResponse validate(@PathVariable Long id) {
        return frameworkLibraryService.validate(id);
    }

    @PostMapping("/{id}/publish")
    public FrameworkSummaryResponse publish(@PathVariable Long id) {
        return frameworkLibraryService.publish(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        frameworkLibraryService.deleteFramework(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/import-bundle")
    public FrameworkBundleStatusResponse bundleStatus() {
        return frameworkBundleImportService.status();
    }

    @PostMapping("/import-bundle")
    public FrameworkBundleImportResultResponse importBundle(
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(required = false) String moduleFolder,
            @RequestParam(required = false) String relativePath,
            @RequestParam(required = false, defaultValue = "false") boolean dryRun) {
        FrameworkBundleImportService.Scope parsed;
        try {
            parsed = FrameworkBundleImportService.Scope.valueOf(scope.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "scope must be FRAMEWORK, MODULE, or ALL");
        }
        return frameworkBundleImportService.importBundle(parsed, moduleFolder, relativePath, dryRun);
    }

    @GetMapping("/{id:\\d+}/tenants")
    public List<FrameworkTenantAssignmentResponse> listTenants(@PathVariable Long id) {
        return frameworkLibraryService.listTenantAssignments(id);
    }

    @PutMapping("/{id:\\d+}/tenants")
    public List<FrameworkTenantAssignmentResponse> updateTenants(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFrameworkTenantsRequest request) {
        return frameworkLibraryService.updateTenantAssignments(id, request);
    }

    @GetMapping("/{id:\\d+}/questions")
    public List<FrameworkQuestionResponse> listQuestions(@PathVariable Long id) {
        return frameworkQuestionService.listActive(id);
    }

    @GetMapping("/{id:\\d+}/questions/sample.xlsx")
    public ResponseEntity<byte[]> questionsSample(@PathVariable Long id) {
        byte[] bytes = frameworkQuestionService.sampleExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionSupport.attachment("framework-" + id + "-questions-sample.xlsx"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/{id:\\d+}/questions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FrameworkQuestionImportResult importQuestions(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return frameworkQuestionService.importExcel(id, file);
    }

    @GetMapping("/{id:\\d+}/risk-library")
    public List<RiskTemplateResponse> listRiskLibrary(@PathVariable Long id) {
        return riskLibraryService.listActive(id);
    }

    @GetMapping("/{id:\\d+}/risk-library/sample.xlsx")
    public ResponseEntity<byte[]> riskLibrarySample(@PathVariable Long id) {
        byte[] bytes = riskLibraryService.sampleExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionSupport.attachment("framework-" + id + "-risk-library-sample.xlsx"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/{id:\\d+}/risk-library/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RiskTemplateImportResult importRiskLibrary(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return riskLibraryService.importExcel(id, file);
    }

    // --- Phase 6C: catalog content editor (Super Admin) ---

    @PutMapping("/{id:\\d+}/nodes/{nodeId}")
    public FrameworkNodeResponse updateNode(@PathVariable Long id,
                                            @PathVariable Long nodeId,
                                            @Valid @RequestBody UpdateFrameworkNodeRequest body) {
        return frameworkContentEditService.updateNode(id, nodeId, body, actorEmail());
    }

    @PostMapping("/{id:\\d+}/nodes")
    public FrameworkNodeResponse createNode(@PathVariable Long id,
                                            @Valid @RequestBody CreateFrameworkNodeRequest body) {
        return frameworkContentEditService.createNode(id, body, actorEmail());
    }

    @PutMapping("/{id:\\d+}/nodes/reorder")
    public FrameworkDetailResponse reorderNodes(@PathVariable Long id,
                                                @Valid @RequestBody ReorderFrameworkNodesRequest body) {
        return frameworkContentEditService.reorder(id, body, actorEmail());
    }

    @PutMapping("/{id:\\d+}/sheet-tabs/rename")
    public FrameworkDetailResponse renameSheetTab(@PathVariable Long id,
                                                  @Valid @RequestBody RenameSheetTabRequest body) {
        return frameworkContentEditService.renameSheetTab(id, body, actorEmail());
    }

    @PostMapping("/{id:\\d+}/nodes/{nodeId}/retire")
    public FrameworkNodeResponse retireNode(@PathVariable Long id, @PathVariable Long nodeId) {
        return frameworkContentEditService.retireNode(id, nodeId, actorEmail());
    }

    @PostMapping("/{id:\\d+}/nodes/{nodeId}/unretire")
    public FrameworkNodeResponse unretireNode(@PathVariable Long id, @PathVariable Long nodeId) {
        return frameworkContentEditService.unretireNode(id, nodeId, actorEmail());
    }

    @DeleteMapping("/{id:\\d+}/nodes/{nodeId}")
    public FrameworkDetailResponse deleteNode(@PathVariable Long id, @PathVariable Long nodeId) {
        return frameworkContentEditService.deleteOrRetireNode(id, nodeId, actorEmail());
    }

    private static String actorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
