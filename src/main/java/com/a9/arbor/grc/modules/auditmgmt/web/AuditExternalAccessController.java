package com.a9.arbor.grc.modules.auditmgmt.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditExternalAccessService;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateExternalAccessInviteRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.ExternalAccessInviteResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.RedeemExternalAccessRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.ValidateExternalAccessResponse;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/audits")
public class AuditExternalAccessController {

    private final AuditExternalAccessService externalAccessService;
    private final UserService userService;

    public AuditExternalAccessController(AuditExternalAccessService externalAccessService,
                                         UserService userService) {
        this.externalAccessService = externalAccessService;
        this.userService = userService;
    }

    @PostMapping("/{id}/external-invites")
    @RequiresModule(ModuleCode.AUDIT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ExternalAccessInviteResponse invite(Authentication authentication,
                                               @PathVariable long id,
                                               @Valid @RequestBody CreateExternalAccessInviteRequest request) {
        return externalAccessService.invite(
                admin(authentication), id, request.name(), request.email(), request.expiryDays());
    }

    @GetMapping("/{id}/external-invites")
    @RequiresModule(ModuleCode.AUDIT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<ExternalAccessInviteResponse> listInvites(Authentication authentication, @PathVariable long id) {
        return externalAccessService.listInvites(admin(authentication), id);
    }

    /** Public: validates invite token before OTP entry (tenant in query). */
    @GetMapping("/external-access/validate")
    public ValidateExternalAccessResponse validate(@RequestParam String tenantId,
                                                   @RequestParam String token) {
        return externalAccessService.validate(tenantId, token);
    }

    /** Public: redeem OTP and receive scoped JWT for the assigned audit. */
    @PostMapping("/external-access/redeem")
    public LoginResponse redeem(@Valid @RequestBody RedeemExternalAccessRequest request) {
        return externalAccessService.redeem(request.tenantId(), request.token(), request.otp());
    }

    private User admin(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
