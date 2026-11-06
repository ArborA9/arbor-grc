package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.RopaProjectService;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaProcessImportResultResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaProjectDtos;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateRopaRequest;
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

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/privacy/ropa-projects")
@RequiresModule(ModuleCode.PRIVACY)
public class RopaProjectController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";
    private static final String ADMIN = "hasAnyRole('OWNER','ADMIN')";

    private final RopaProjectService service;
    private final UserService userService;

    public RopaProjectController(RopaProjectService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<RopaProjectDtos.ProjectResponse> list() {
        return service.listProjects();
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public RopaProjectDtos.ProjectResponse get(@PathVariable long id) {
        return service.getProject(id);
    }

    @PostMapping
    @PreAuthorize(ADMIN)
    public RopaProjectDtos.ProjectResponse create(Authentication auth,
                                                  @Valid @RequestBody RopaProjectDtos.CreateProjectRequest req) {
        return service.createProject(user(auth), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN)
    public RopaProjectDtos.ProjectResponse rename(Authentication auth, @PathVariable long id,
                                                  @Valid @RequestBody RopaProjectDtos.CreateProjectRequest req) {
        return service.renameProject(user(auth), id, req);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(ADMIN)
    public RopaProjectDtos.ProjectResponse complete(Authentication auth, @PathVariable long id) {
        return service.completeProject(user(auth), id);
    }

    @PostMapping("/{projectId}/departments")
    @PreAuthorize(ADMIN)
    public RopaProjectDtos.DepartmentResponse addDepartment(Authentication auth, @PathVariable long projectId,
                                                            @Valid @RequestBody RopaProjectDtos.AddDepartmentRequest req) {
        return service.addDepartment(user(auth), projectId, req);
    }

    @PostMapping("/departments/{departmentId}/assign")
    @PreAuthorize(ADMIN)
    public RopaProjectDtos.DepartmentResponse assign(Authentication auth, @PathVariable long departmentId,
                                                     @RequestBody RopaProjectDtos.AssignDepartmentRequest req) {
        return service.assignDepartment(user(auth), departmentId, req);
    }

    @GetMapping("/departments/{departmentId}/processes")
    @PreAuthorize(READ_ROLES)
    public List<RopaResponse> processes(@PathVariable long departmentId) {
        return service.listProcesses(departmentId);
    }

    @GetMapping("/departments/{departmentId}/processes/template.xlsx")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public ResponseEntity<byte[]> processTemplate(Authentication auth, @PathVariable long departmentId) {
        var file = service.downloadProcessTemplate(user(auth), departmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping(value = "/departments/{departmentId}/processes/import.xlsx",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RopaProcessImportResultResponse importProcesses(Authentication auth,
                                                           @PathVariable long departmentId,
                                                           @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an Arbor ROPA process Excel file");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an .xlsx file from the Arbor template");
        }
        try {
            return service.importProcesses(user(auth), departmentId, file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
    }

    @PostMapping("/departments/{departmentId}/processes")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RopaResponse createProcess(Authentication auth, @PathVariable long departmentId,
                                      @Valid @RequestBody CreateRopaRequest req) {
        return service.createProcess(user(auth), departmentId, req);
    }

    @PutMapping("/departments/{departmentId}/processes/{processId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RopaResponse updateProcess(Authentication auth, @PathVariable long departmentId,
                                      @PathVariable long processId,
                                      @Valid @RequestBody UpdateRopaRequest req) {
        return service.updateProcess(user(auth), departmentId, processId, req);
    }

    @PostMapping("/departments/{departmentId}/submit")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RopaProjectDtos.DepartmentResponse submit(Authentication auth, @PathVariable long departmentId) {
        return service.submitDepartment(user(auth), departmentId);
    }

    @PostMapping("/departments/{departmentId}/processes/{processId}/review")
    @PreAuthorize(ADMIN)
    public RopaResponse review(Authentication auth, @PathVariable long departmentId,
                               @PathVariable long processId,
                               @Valid @RequestBody RopaProjectDtos.ReviewProcessRequest req) {
        return service.reviewProcess(user(auth), departmentId, processId, req);
    }

    @PostMapping("/departments/{departmentId}/finish-review")
    @PreAuthorize(ADMIN)
    public RopaProjectDtos.DepartmentResponse finishReview(Authentication auth, @PathVariable long departmentId) {
        return service.finishReview(user(auth), departmentId);
    }

    @PostMapping("/departments/{departmentId}/send-to-head")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public RopaProjectDtos.DepartmentResponse sendToHead(Authentication auth, @PathVariable long departmentId) {
        return service.sendToHead(user(auth), departmentId);
    }

    @PostMapping("/departments/{departmentId}/processes/{processId}/head-review")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RopaResponse headReview(Authentication auth, @PathVariable long departmentId,
                                   @PathVariable long processId,
                                   @Valid @RequestBody RopaProjectDtos.HeadReviewProcessRequest req) {
        return service.headReviewProcess(user(auth), departmentId, processId, req);
    }

    @PostMapping("/departments/{departmentId}/head-approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RopaProjectDtos.DepartmentResponse headApprove(Authentication auth, @PathVariable long departmentId,
                                                          @RequestBody(required = false) RopaProjectDtos.DecideRequest req) {
        return service.headApprove(user(auth), departmentId, req);
    }

    @PostMapping("/departments/{departmentId}/head-return")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RopaProjectDtos.DepartmentResponse headReturn(Authentication auth, @PathVariable long departmentId,
                                                         @RequestBody RopaProjectDtos.DecideRequest req) {
        return service.headReturn(user(auth), departmentId, req);
    }

    @PostMapping("/departments/{departmentId}/close")
    @PreAuthorize(ADMIN)
    public RopaProjectDtos.DepartmentResponse close(Authentication auth, @PathVariable long departmentId) {
        return service.closeDepartment(user(auth), departmentId);
    }

    private User user(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    }
}
