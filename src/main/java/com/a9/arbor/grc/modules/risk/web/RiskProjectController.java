package com.a9.arbor.grc.modules.risk.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.service.RiskProjectService;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskProjectRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskProjectResponse;
import com.a9.arbor.grc.modules.risk.web.dto.UpdateRiskProjectStatusRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/risk-projects")
@RequiresModule({ModuleCode.ISMS, ModuleCode.ERM, ModuleCode.PRIVACY})
public class RiskProjectController {

    private final RiskProjectService projectService;
    private final UserService userService;

    public RiskProjectController(RiskProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<RiskProjectResponse> list(Authentication authentication, @RequestParam RiskType projectType) {
        return projectService.list(currentUser(authentication), projectType);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public RiskProjectResponse get(@PathVariable long id) {
        return projectService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskProjectResponse create(Authentication authentication,
                                      @Valid @RequestBody CreateRiskProjectRequest request) {
        return projectService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RiskProjectResponse updateStatus(Authentication authentication,
                                            @PathVariable long id,
                                            @Valid @RequestBody UpdateRiskProjectStatusRequest request) {
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
