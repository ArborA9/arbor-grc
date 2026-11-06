package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.DpiaProjectService;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaProjectDtos;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/privacy/dpia-projects")
@RequiresModule(ModuleCode.PRIVACY)
public class DpiaProjectController {

    private static final String READ =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";
    private static final String ADMIN = "hasAnyRole('OWNER','ADMIN')";
    private static final String FILL = "hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')";
    private static final String HEAD =
            "hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')";

    private final DpiaProjectService service;
    private final UserService userService;

    public DpiaProjectController(DpiaProjectService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ)
    public List<DpiaProjectDtos.ProjectResponse> list(Authentication auth) {
        return service.listProjects(user(auth));
    }

    @GetMapping("/risk-title-candidates")
    @PreAuthorize(READ)
    public List<DpiaProjectDtos.RiskTitleCandidate> riskTitleCandidates(Authentication auth) {
        return service.listRiskTitleCandidates(user(auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ)
    public DpiaProjectDtos.ProjectResponse get(Authentication auth, @PathVariable long id) {
        return service.getProject(user(auth), id);
    }

    @PostMapping
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.ProjectResponse create(Authentication auth,
                                                  @Valid @RequestBody DpiaProjectDtos.CreateProjectRequest req) {
        return service.createProject(user(auth), req);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.ProjectResponse complete(Authentication auth, @PathVariable long id) {
        return service.completeProject(user(auth), id);
    }

    @PostMapping("/{projectId}/departments")
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.DepartmentResponse addDepartment(Authentication auth, @PathVariable long projectId,
                                                            @Valid @RequestBody DpiaProjectDtos.AddDepartmentRequest req) {
        return service.addDepartment(user(auth), projectId, req);
    }

    @GetMapping("/departments/{departmentId}")
    @PreAuthorize(READ)
    public DpiaProjectDtos.DepartmentResponse getDepartment(Authentication auth, @PathVariable long departmentId) {
        return service.getDepartment(user(auth), departmentId);
    }

    @PostMapping("/departments/{departmentId}/assign")
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.DepartmentResponse assign(Authentication auth, @PathVariable long departmentId,
                                                     @RequestBody DpiaProjectDtos.AssignDepartmentRequest req) {
        return service.assign(user(auth), departmentId, req);
    }

    @PutMapping("/departments/{departmentId}/answers")
    @PreAuthorize(FILL)
    public DpiaProjectDtos.DepartmentResponse saveAnswers(Authentication auth, @PathVariable long departmentId,
                                                          @RequestBody DpiaProjectDtos.SaveAnswersRequest req) {
        return service.saveAnswers(user(auth), departmentId, req);
    }

    @PostMapping("/departments/{departmentId}/submit")
    @PreAuthorize(FILL)
    public DpiaProjectDtos.DepartmentResponse submit(Authentication auth, @PathVariable long departmentId) {
        return service.submit(user(auth), departmentId);
    }

    @PostMapping("/departments/{departmentId}/review")
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.DepartmentResponse review(Authentication auth, @PathVariable long departmentId,
                                                     @Valid @RequestBody DpiaProjectDtos.ReviewRequest req) {
        return service.review(user(auth), departmentId, req);
    }

    @PutMapping("/departments/{departmentId}/assessment")
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.DepartmentResponse saveAssessment(Authentication auth, @PathVariable long departmentId,
                                                             @RequestBody DpiaProjectDtos.SaveAssessmentRequest req) {
        return service.saveAssessment(user(auth), departmentId, req);
    }

    @PostMapping("/departments/{departmentId}/finalize-assessment")
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.DepartmentResponse finalizeAssessment(Authentication auth, @PathVariable long departmentId) {
        return service.finalizeAssessment(user(auth), departmentId);
    }

    @PostMapping("/departments/{departmentId}/head-approve")
    @PreAuthorize(HEAD)
    public DpiaProjectDtos.DepartmentResponse headApprove(Authentication auth, @PathVariable long departmentId,
                                                          @RequestBody(required = false) DpiaProjectDtos.HeadDecideRequest req) {
        return service.headApprove(user(auth), departmentId, req);
    }

    @PostMapping("/departments/{departmentId}/head-return")
    @PreAuthorize(HEAD)
    public DpiaProjectDtos.DepartmentResponse headReturn(Authentication auth, @PathVariable long departmentId,
                                                         @RequestBody(required = false) DpiaProjectDtos.HeadDecideRequest req) {
        return service.headReturn(user(auth), departmentId, req);
    }

    @PostMapping("/departments/{departmentId}/close")
    @PreAuthorize(ADMIN)
    public DpiaProjectDtos.DepartmentResponse close(Authentication auth, @PathVariable long departmentId) {
        return service.close(user(auth), departmentId);
    }

    private User user(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
    }
}
