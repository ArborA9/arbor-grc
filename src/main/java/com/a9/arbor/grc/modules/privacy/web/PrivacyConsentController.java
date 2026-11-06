package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.PrivacyConsentService;
import com.a9.arbor.grc.modules.privacy.web.dto.ConsentResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateConsentRequest;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/privacy/consent")
@RequiresModule(ModuleCode.PRIVACY)
public class PrivacyConsentController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";

    private final PrivacyConsentService consentService;
    private final UserService userService;

    public PrivacyConsentController(PrivacyConsentService consentService, UserService userService) {
        this.consentService = consentService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<ConsentResponse> list() {
        return consentService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public ConsentResponse get(@PathVariable long id) {
        return consentService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ConsentResponse create(Authentication authentication, @Valid @RequestBody CreateConsentRequest request) {
        return consentService.create(adminUser(authentication), request);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ConsentResponse withdraw(Authentication authentication, @PathVariable long id) {
        return consentService.withdraw(adminUser(authentication), id);
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
