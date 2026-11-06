package com.a9.arbor.grc.modules.bcms.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.bcms.domain.BiaStatus;
import com.a9.arbor.grc.modules.bcms.service.BcmsLifecycleScheduler;
import com.a9.arbor.grc.modules.bcms.service.BiaService;
import com.a9.arbor.grc.modules.bcms.web.dto.AssignBiaRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaCycleResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaLookupsResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaProcessImportResultResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaProcessResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaProjectResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateBiaCycleRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateBiaProjectRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.HeadDecideBiaRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.HeadVerifyRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.ReviewBiaProcessRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SaveBiaImpactRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SaveBiaMorRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SendBiaToHeadRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.UpdateBiaCycleRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.UpsertBiaProcessRequest;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bcms/bia")
@RequiresModule(ModuleCode.BCMS)
public class BiaController {

    private final BiaService biaService;
    private final UserService userService;
    private final BcmsLifecycleScheduler bcmsLifecycleScheduler;

    public BiaController(BiaService biaService,
                         UserService userService,
                         BcmsLifecycleScheduler bcmsLifecycleScheduler) {
        this.biaService = biaService;
        this.userService = userService;
        this.bcmsLifecycleScheduler = bcmsLifecycleScheduler;
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<BiaProjectResponse> listProjects() {
        return biaService.listProjects();
    }

    @GetMapping("/projects/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public BiaProjectResponse getProject(@PathVariable long id) {
        return biaService.getProject(id);
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BiaProjectResponse createProject(Authentication authentication,
                                            @Valid @RequestBody CreateBiaProjectRequest request) {
        return biaService.createProject(adminUser(authentication), request);
    }

    @GetMapping("/lookups")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public BiaLookupsResponse lookups() {
        return biaService.lookups();
    }

    @GetMapping("/cycles")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<BiaCycleResponse> list(Authentication authentication,
                                       @RequestParam(required = false) Long biaProjectId,
                                       @RequestParam(required = false) Long orgUnitId,
                                       @RequestParam(required = false) BiaStatus status) {
        return biaService.list(currentUser(authentication), biaProjectId, orgUnitId, status);
    }

    @GetMapping("/cycles/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public BiaCycleResponse get(Authentication authentication, @PathVariable long id) {
        return biaService.get(currentUser(authentication), id);
    }

    @PostMapping("/cycles")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BiaCycleResponse create(Authentication authentication, @Valid @RequestBody CreateBiaCycleRequest request) {
        return biaService.create(adminUser(authentication), request);
    }

    @PutMapping("/cycles/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BiaCycleResponse updateCycle(Authentication authentication,
                                        @PathVariable long id,
                                        @Valid @RequestBody UpdateBiaCycleRequest request) {
        return biaService.updateCycle(adminUser(authentication), id, request);
    }

    @PostMapping("/cycles/{id:\\d+}/assign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BiaCycleResponse assign(Authentication authentication,
                                   @PathVariable long id,
                                   @Valid @RequestBody AssignBiaRequest request) {
        return biaService.assign(adminUser(authentication), id, request);
    }

    @PostMapping("/cycles/{id:\\d+}/submit")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaCycleResponse submit(Authentication authentication, @PathVariable long id) {
        return biaService.submit(currentUser(authentication), id);
    }

    @PostMapping("/cycles/{id:\\d+}/send-to-head")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BiaCycleResponse sendToHead(Authentication authentication,
                                       @PathVariable long id,
                                       @Valid @RequestBody SendBiaToHeadRequest request) {
        return biaService.sendToHead(adminUser(authentication), id, request);
    }

    @PostMapping("/cycles/{id:\\d+}/head-verify")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER')")
    public BiaCycleResponse headVerify(Authentication authentication,
                                       @PathVariable long id,
                                       @RequestBody HeadVerifyRequest request) {
        return biaService.headVerify(currentUser(authentication), id, request);
    }

    @PostMapping("/cycles/{id:\\d+}/head-approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER')")
    public BiaCycleResponse headApprove(Authentication authentication,
                                        @PathVariable long id,
                                        @RequestBody(required = false) HeadDecideBiaRequest request) {
        return biaService.headApprove(currentUser(authentication), id,
                request != null ? request : new HeadDecideBiaRequest(null));
    }

    @PostMapping("/cycles/{id:\\d+}/head-return")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER')")
    public BiaCycleResponse headReturn(Authentication authentication,
                                       @PathVariable long id,
                                       @Valid @RequestBody HeadDecideBiaRequest request) {
        return biaService.headReturn(currentUser(authentication), id, request);
    }

    @PostMapping("/cycles/{id:\\d+}/close")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BiaCycleResponse close(Authentication authentication, @PathVariable long id) {
        return biaService.close(adminUser(authentication), id);
    }

    @PostMapping("/cycles/{id:\\d+}/processes")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaProcessResponse createProcess(Authentication authentication,
                                            @PathVariable long id,
                                            @Valid @RequestBody UpsertBiaProcessRequest request) {
        return biaService.createProcess(currentUser(authentication), id, request);
    }

    @PutMapping("/processes/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaProcessResponse updateProcess(Authentication authentication,
                                            @PathVariable long id,
                                            @Valid @RequestBody UpsertBiaProcessRequest request) {
        return biaService.updateProcess(currentUser(authentication), id, request);
    }

    @DeleteMapping("/processes/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaCycleResponse deleteProcess(Authentication authentication, @PathVariable long id) {
        return biaService.deleteProcess(currentUser(authentication), id);
    }

    @PutMapping("/processes/{id:\\d+}/impact")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaProcessResponse saveImpact(Authentication authentication,
                                         @PathVariable long id,
                                         @Valid @RequestBody SaveBiaImpactRequest request) {
        return biaService.saveImpact(currentUser(authentication), id, request);
    }

    @PutMapping("/processes/{id:\\d+}/mor")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaProcessResponse saveMor(Authentication authentication,
                                      @PathVariable long id,
                                      @Valid @RequestBody SaveBiaMorRequest request) {
        return biaService.saveMor(currentUser(authentication), id, request);
    }

    @PostMapping("/processes/{id:\\d+}/review")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BiaProcessResponse review(Authentication authentication,
                                     @PathVariable long id,
                                     @Valid @RequestBody ReviewBiaProcessRequest request) {
        return biaService.reviewProcess(adminUser(authentication), id, request);
    }

    @GetMapping("/processes/approved")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<BiaProcessResponse> approvedProcesses() {
        return biaService.listApprovedProcesses();
    }

    @GetMapping("/cycles/{id:\\d+}/export.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR')")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication, @PathVariable long id) {
        var file = biaService.exportExcel(currentUser(authentication), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/cycles/{id:\\d+}/template.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public ResponseEntity<byte[]> processTemplate(Authentication authentication, @PathVariable long id) {
        var file = biaService.downloadProcessTemplate(currentUser(authentication), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @GetMapping("/template.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public ResponseEntity<byte[]> template(Authentication authentication) {
        var file = biaService.downloadTemplate(currentUser(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping(value = "/cycles/{id:\\d+}/processes/import.xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaProcessImportResultResponse importProcesses(Authentication authentication,
                                                          @PathVariable long id,
                                                          @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an Arbor BIA process Excel file");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an .xlsx file from the Arbor template");
        }
        try {
            return biaService.importProcesses(currentUser(authentication), id, file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
    }

    @PostMapping(value = "/cycles/{id:\\d+}/import.xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public BiaCycleResponse importExcel(Authentication authentication,
                                        @PathVariable long id,
                                        @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an Arbor BIA Excel file");
        }
        try {
            return biaService.importExcel(currentUser(authentication), id, file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
    }

    @PostMapping("/jobs/lifecycle-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = bcmsLifecycleScheduler.runCurrentTenant(day);
        return Map.of(
                "asOf", day.toString(),
                "biaNotSubmitted", result.biaNotSubmitted(),
                "biaReviewDue", result.biaReviewDue(),
                "bcpReviewDue", result.bcpReviewDue(),
                "bcpExpired", result.bcpExpired(),
                "exerciseDue", result.exerciseDue(),
                "exerciseOverdue", result.exerciseOverdue());
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
}
