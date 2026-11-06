package com.a9.arbor.grc.modules.bcms.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.bcms.service.DeptProfileService;
import com.a9.arbor.grc.modules.bcms.web.dto.BcmsDeptProfileResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.UpsertDeptProfileRequest;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/bcms/dept-profiles")
@RequiresModule(ModuleCode.BCMS)
public class BcmsDeptProfileController {

    private final DeptProfileService deptProfileService;
    private final UserService userService;

    public BcmsDeptProfileController(DeptProfileService deptProfileService, UserService userService) {
        this.deptProfileService = deptProfileService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<BcmsDeptProfileResponse> list() {
        return deptProfileService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public BcmsDeptProfileResponse get(@PathVariable long id) {
        return deptProfileService.get(id);
    }

    @GetMapping("/by-org-unit/{orgUnitId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public BcmsDeptProfileResponse getByOrgUnit(@PathVariable long orgUnitId) {
        return deptProfileService.getByOrgUnit(orgUnitId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BcmsDeptProfileResponse upsert(Authentication authentication,
                                          @Valid @RequestBody UpsertDeptProfileRequest request) {
        return deptProfileService.upsert(adminUser(authentication), request);
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
