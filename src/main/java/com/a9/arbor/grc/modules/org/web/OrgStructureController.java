package com.a9.arbor.grc.modules.org.web;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.org.service.DepartmentSyncService;
import com.a9.arbor.grc.modules.org.service.OrgStructureService;
import com.a9.arbor.grc.modules.org.web.dto.DepartmentSyncResponse;
import com.a9.arbor.grc.modules.org.web.dto.CreateOrgUnitRequest;
import com.a9.arbor.grc.modules.org.web.dto.OrgLevelResponse;
import com.a9.arbor.grc.modules.org.web.dto.OrgProfileResponse;
import com.a9.arbor.grc.modules.org.web.dto.OrgUnitResponse;
import com.a9.arbor.grc.modules.org.web.dto.SessionPolicyResponse;
import com.a9.arbor.grc.modules.org.web.dto.UpdateOrgLevelsRequest;
import com.a9.arbor.grc.modules.org.web.dto.UpdateOrgProfileRequest;
import com.a9.arbor.grc.modules.org.web.dto.UpdateOrgUnitRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Organisation structure & guided-setup profile. Platform always-on. Vendors have no access.
 */
@RestController
@RequestMapping("/api/org")
public class OrgStructureController {

    private final OrgStructureService orgStructureService;
    private final DepartmentSyncService departmentSyncService;
    private final UserService userService;

    public OrgStructureController(OrgStructureService orgStructureService,
                                  DepartmentSyncService departmentSyncService,
                                  UserService userService) {
        this.orgStructureService = orgStructureService;
        this.departmentSyncService = departmentSyncService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR')")
    public OrgProfileResponse profile() {
        return orgStructureService.getProfile();
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public OrgProfileResponse updateProfile(Authentication authentication,
                                            @Valid @RequestBody UpdateOrgProfileRequest request) {
        return orgStructureService.updateProfile(adminUser(authentication), request);
    }

    /** Path C B3: any authenticated tenant user (incl. Contributor/Vendor) can read the idle timeout. */
    @GetMapping("/session-policy")
    @PreAuthorize("isAuthenticated()")
    public SessionPolicyResponse sessionPolicy() {
        return orgStructureService.sessionPolicy();
    }

    @GetMapping("/levels")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR')")
    public List<OrgLevelResponse> levels() {
        return orgStructureService.listLevels();
    }

    @PutMapping("/levels")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<OrgLevelResponse> updateLevels(Authentication authentication,
                                               @Valid @RequestBody UpdateOrgLevelsRequest request) {
        return orgStructureService.updateLevels(adminUser(authentication), request);
    }

    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR')")
    public List<OrgUnitResponse> units(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return orgStructureService.tree(includeInactive);
    }

    @PostMapping("/departments/sync")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DepartmentSyncResponse syncDepartments(Authentication authentication) {
        return departmentSyncService.sync(adminUser(authentication));
    }

    @PostMapping("/units")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public OrgUnitResponse createUnit(Authentication authentication,
                                      @Valid @RequestBody CreateOrgUnitRequest request) {
        return orgStructureService.createUnit(adminUser(authentication), request);
    }

    @PutMapping("/units/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public OrgUnitResponse updateUnit(Authentication authentication,
                                      @PathVariable long id,
                                      @Valid @RequestBody UpdateOrgUnitRequest request) {
        return orgStructureService.updateUnit(adminUser(authentication), id, request);
    }

    @PostMapping("/units/{id}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public OrgUnitResponse deactivate(Authentication authentication, @PathVariable long id) {
        return orgStructureService.deactivateUnit(adminUser(authentication), id);
    }

    @PostMapping("/units/{id}/reactivate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public OrgUnitResponse reactivate(Authentication authentication, @PathVariable long id) {
        return orgStructureService.reactivateUnit(adminUser(authentication), id);
    }

    @DeleteMapping("/units/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void deleteUnit(Authentication authentication, @PathVariable long id) {
        orgStructureService.deleteUnit(adminUser(authentication), id);
    }

    @PostMapping("/wizard/complete")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public OrgProfileResponse completeWizard(Authentication authentication) {
        return orgStructureService.completeWizard(adminUser(authentication));
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
