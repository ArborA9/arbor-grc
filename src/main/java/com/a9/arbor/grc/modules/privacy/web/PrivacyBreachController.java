package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.PrivacyBreachClockScheduler;
import com.a9.arbor.grc.modules.privacy.service.PrivacyBreachService;
import com.a9.arbor.grc.modules.privacy.web.dto.BreachResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateBreachRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.ReportBreachRequest;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/privacy/breaches")
@RequiresModule(ModuleCode.PRIVACY)
public class PrivacyBreachController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";

    private final PrivacyBreachService breachService;
    private final PrivacyBreachClockScheduler breachClockScheduler;
    private final UserService userService;

    public PrivacyBreachController(PrivacyBreachService breachService,
                                   PrivacyBreachClockScheduler breachClockScheduler,
                                   UserService userService) {
        this.breachService = breachService;
        this.breachClockScheduler = breachClockScheduler;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<BreachResponse> list() {
        return breachService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public BreachResponse get(@PathVariable long id) {
        return breachService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BreachResponse create(Authentication authentication, @Valid @RequestBody CreateBreachRequest request) {
        return breachService.create(adminUser(authentication), request);
    }

    @PostMapping("/{id}/investigate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BreachResponse investigate(Authentication authentication, @PathVariable long id) {
        return breachService.startInvestigation(adminUser(authentication), id);
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BreachResponse report(Authentication authentication,
                                 @PathVariable long id,
                                 @RequestBody(required = false) ReportBreachRequest request) {
        return breachService.reportToAuthority(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BreachResponse close(Authentication authentication, @PathVariable long id) {
        return breachService.close(adminUser(authentication), id);
    }

    @PostMapping("/lifecycle/run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        Instant instant = asOf == null || asOf.isBlank() ? Instant.now() : Instant.parse(asOf);
        var result = breachClockScheduler.runCurrentTenant(instant);
        return Map.of(
                "asOf", instant.toString(),
                "hour48", result.hour48(),
                "overdue", result.overdue());
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
