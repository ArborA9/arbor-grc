package com.a9.arbor.grc.modules.project.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.project.service.ComplianceProjectService;
import com.a9.arbor.grc.modules.project.web.dto.ComplianceProjectDetailResponse;
import com.a9.arbor.grc.modules.project.web.dto.ComplianceProjectSummaryResponse;
import com.a9.arbor.grc.modules.project.web.dto.CreateComplianceProjectRequest;
import com.a9.arbor.grc.modules.project.web.dto.UpdateComplianceProjectStatusRequest;
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
@RequestMapping("/api/compliance-projects")
@RequiresModule({
        ModuleCode.ISMS, ModuleCode.BCMS, ModuleCode.QMS, ModuleCode.OHSMS, ModuleCode.SAMA,
        ModuleCode.ITSM, ModuleCode.AIMS, ModuleCode.PRIVACY
})
public class ComplianceProjectController {

    private final ComplianceProjectService projectService;
    private final UserService userService;

    public ComplianceProjectController(ComplianceProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<ComplianceProjectSummaryResponse> list(Authentication authentication) {
        return projectService.list(currentUser(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public ComplianceProjectDetailResponse get(Authentication authentication, @PathVariable long id) {
        return projectService.get(currentUser(authentication), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ComplianceProjectDetailResponse create(Authentication authentication,
                                                  @Valid @RequestBody CreateComplianceProjectRequest request) {
        return projectService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ComplianceProjectDetailResponse updateStatus(Authentication authentication,
                                                        @PathVariable long id,
                                                        @Valid @RequestBody UpdateComplianceProjectStatusRequest request) {
        return projectService.updateStatus(adminUser(authentication), id, request);
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
