package com.a9.arbor.grc.modules.iam.web;

import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.iam.service.ContributorEligibilityService;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.iam.service.PasswordResetService;
import com.a9.arbor.grc.modules.iam.web.dto.CreateUserRequest;
import com.a9.arbor.grc.modules.iam.web.dto.EnableLocalLoginRequest;
import com.a9.arbor.grc.modules.iam.web.dto.LocalLoginPolicyResponse;
import com.a9.arbor.grc.modules.iam.web.dto.ResetUserPasswordRequest;
import com.a9.arbor.grc.modules.iam.web.dto.UpdateUserOrgUnitsRequest;
import com.a9.arbor.grc.modules.iam.web.dto.UpdateUserRolesRequest;
import com.a9.arbor.grc.modules.iam.web.dto.UserDirectoryResponse;
import com.a9.arbor.grc.modules.iam.web.dto.UserResponse;
import com.a9.arbor.grc.modules.risk.web.dto.EligibleContributorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Tenant-scoped user management. Creating users is restricted to OWNER/ADMIN (RBAC enforced at the
 * API layer via {@link PreAuthorize}).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ContributorEligibilityService contributorEligibilityService;
    private final PasswordResetService passwordResetService;
    private final ArborNotificationProperties notificationProperties;
    private final ArborSecurityProperties securityProperties;
    private final AdminModuleAccessService adminModuleAccessService;

    public UserController(UserService userService,
                          ContributorEligibilityService contributorEligibilityService,
                          PasswordResetService passwordResetService,
                          ArborNotificationProperties notificationProperties,
                          ArborSecurityProperties securityProperties,
                          AdminModuleAccessService adminModuleAccessService) {
        this.userService = userService;
        this.contributorEligibilityService = contributorEligibilityService;
        this.passwordResetService = passwordResetService;
        this.notificationProperties = notificationProperties;
        this.securityProperties = securityProperties;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    /**
     * Invite an SSO user (default: identity from the tenant's AD) or create a LOCAL break-glass
     * account (password required). Either way, roles are assigned here: Arbor owns authorization.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user;
        if (request.authTypeOrDefault() == AuthType.LOCAL) {
            if (request.password() == null || request.password().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "password (min 8 characters) is required for LOCAL accounts");
            }
            user = userService.createUser(
                    request.email(), request.password(), request.fullName(), request.roles(),
                    request.orgUnitIds(), request.adminModules());
        } else {
            user = userService.inviteSsoUser(
                    request.email(), request.fullName(), request.roles(), request.orgUnitIds(),
                    request.adminModules());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<UserResponse> list() {
        return userService.listUsers().stream().map(this::toResponse).toList();
    }

    /** Id / name / email for assignment and approver pickers. Champion can read this. */
    @GetMapping("/directory")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<UserDirectoryResponse> directory() {
        return userService.listDirectoryUsers().stream().map(UserDirectoryResponse::from).toList();
    }

    /** Lead/team pickers when planning internal audits (Journey 12: Internal Auditor can plan). */
    @GetMapping("/audit-planning")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<UserResponse> listForAuditPlanning() {
        return userService.listAuditPlanningUsers().stream().map(this::toResponse).toList();
    }

    @GetMapping("/eligible-contributors")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<EligibleContributorResponse> eligibleContributors(@RequestParam Long orgUnitId) {
        return contributorEligibilityService.listForOrgUnit(orgUnitId);
    }

    @PutMapping("/{id}/org-units")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse updateOrgUnits(@PathVariable long id,
                                       @Valid @RequestBody UpdateUserOrgUnitsRequest request) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userService.assertVendorNotEditable(user);
        userService.replaceOrgUnits(user, request.orgUnitIds());
        return toResponse(user);
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse disable(Authentication authentication, @PathVariable long id) {
        User actor = requireActor(authentication);
        return toResponse(userService.disableUser(actor, id));
    }

    @GetMapping("/local-login-policy")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public LocalLoginPolicyResponse localLoginPolicy() {
        boolean sso = userService.tenantSsoEnabled();
        boolean ownerOnly = securityProperties.ownerOnlyLocalLogin() && sso;
        return new LocalLoginPolicyResponse(
                securityProperties.ownerOnlyLocalLogin() ? "OWNER_ONLY" : "ALL",
                sso,
                ownerOnly);
    }

    @PostMapping("/{id}/local-login/disable")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse disableLocal(Authentication authentication, @PathVariable long id) {
        User actor = requireActor(authentication);
        return toResponse(userService.disableLocalLogin(actor, id));
    }

    @PostMapping("/{id}/local-login/enable")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse enableLocal(Authentication authentication,
                                    @PathVariable long id,
                                    @RequestBody(required = false) EnableLocalLoginRequest request) {
        User actor = requireActor(authentication);
        User user = userService.enableLocalLogin(actor, id);
        if (notificationProperties.emailLive()) {
            String tenantId = com.a9.arbor.grc.tenancy.TenantContext.getTenantId();
            passwordResetService.issueResetLink(tenantId, user);
        } else {
            String password = request == null ? null : request.password();
            if (password == null || password.length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "password (min 8 characters) is required to turn local login on");
            }
            user = userService.setLocalPassword(user, password);
        }
        return toResponse(user);
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse enable(Authentication authentication, @PathVariable long id) {
        User actor = requireActor(authentication);
        return toResponse(userService.enableUser(actor, id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse updateRoles(Authentication authentication,
                                    @PathVariable long id,
                                    @Valid @RequestBody UpdateUserRolesRequest request) {
        User actor = requireActor(authentication);
        return toResponse(userService.updateRoles(actor, id, request.roles(), request.adminModules(),
                request.orgUnitIds()));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserResponse resetPassword(Authentication authentication,
                                      @PathVariable long id,
                                      @Valid @RequestBody ResetUserPasswordRequest request) {
        User actor = requireActor(authentication);
        return toResponse(userService.resetPassword(actor, id, request.password()));
    }

    private User requireActor(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.from(user, userService.orgUnitIdsFor(user.getId()),
                adminModuleAccessService.codesFor(user));
    }
}
