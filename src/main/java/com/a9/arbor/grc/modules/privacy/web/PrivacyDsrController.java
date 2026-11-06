package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.PrivacyDsrClockScheduler;
import com.a9.arbor.grc.modules.privacy.service.PrivacyDsrService;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateDsrRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.DsrResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RejectDsrRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateDsrRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/privacy/dsr")
@RequiresModule(ModuleCode.PRIVACY)
public class PrivacyDsrController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";

    private final PrivacyDsrService dsrService;
    private final PrivacyDsrClockScheduler dsrClockScheduler;
    private final UserService userService;

    public PrivacyDsrController(PrivacyDsrService dsrService,
                                PrivacyDsrClockScheduler dsrClockScheduler,
                                UserService userService) {
        this.dsrService = dsrService;
        this.dsrClockScheduler = dsrClockScheduler;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<DsrResponse> list() {
        return dsrService.list();
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize(READ_ROLES)
    public DsrResponse get(@PathVariable long id) {
        return dsrService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DsrResponse create(Authentication authentication,
                              @Valid @RequestBody CreateDsrRequest request) {
        return dsrService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DsrResponse update(Authentication authentication,
                              @PathVariable long id,
                              @Valid @RequestBody UpdateDsrRequest request) {
        return dsrService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id:\\d+}/start")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DsrResponse start(Authentication authentication, @PathVariable long id) {
        return dsrService.start(adminUser(authentication), id);
    }

    @PostMapping("/{id:\\d+}/complete")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DsrResponse complete(Authentication authentication, @PathVariable long id) {
        return dsrService.complete(adminUser(authentication), id);
    }

    @PostMapping("/{id:\\d+}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DsrResponse reject(Authentication authentication,
                              @PathVariable long id,
                              @Valid @RequestBody RejectDsrRequest request) {
        return dsrService.reject(adminUser(authentication), id, request);
    }

    /**
     * Manual PDPL clock run for this tenant (E2E / backdated). Optional {@code asOf=YYYY-MM-DD}.
     */
    @PostMapping("/lifecycle/run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = dsrClockScheduler.runCurrentTenant(day);
        return Map.of(
                "asOf", day.toString(),
                "day20", result.day20(),
                "day28", result.day28(),
                "overdue", result.overdue());
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
