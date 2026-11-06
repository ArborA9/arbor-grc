package com.a9.arbor.grc.modules.kri.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.kri.service.KriLifecycleScheduler;
import com.a9.arbor.grc.modules.kri.service.KriService;
import com.a9.arbor.grc.modules.kri.web.dto.CreateKriRequest;
import com.a9.arbor.grc.modules.kri.web.dto.KriResponse;
import com.a9.arbor.grc.modules.kri.web.dto.RecordKriValueRequest;
import com.a9.arbor.grc.modules.kri.web.dto.UpdateKriRequest;
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
@RequestMapping("/api/kris")
@RequiresModule({ModuleCode.ERM})
public class KriController {

    private final KriService kriService;
    private final KriLifecycleScheduler kriLifecycleScheduler;
    private final UserService userService;

    public KriController(KriService kriService,
                         KriLifecycleScheduler kriLifecycleScheduler,
                         UserService userService) {
        this.kriService = kriService;
        this.kriLifecycleScheduler = kriLifecycleScheduler;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<KriResponse> list(Authentication authentication) {
        return kriService.list(currentUser(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public KriResponse get(Authentication authentication, @PathVariable long id) {
        return kriService.get(currentUser(authentication), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public KriResponse create(Authentication authentication, @Valid @RequestBody CreateKriRequest request) {
        return kriService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public KriResponse update(Authentication authentication,
                              @PathVariable long id,
                              @Valid @RequestBody UpdateKriRequest request) {
        return kriService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/values")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public KriResponse recordValue(Authentication authentication,
                                   @PathVariable long id,
                                   @Valid @RequestBody RecordKriValueRequest request) {
        return kriService.recordValue(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/acknowledge-red")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public KriResponse acknowledgeRed(Authentication authentication, @PathVariable long id) {
        return kriService.acknowledgeRed(adminUser(authentication), id);
    }

    @PostMapping("/jobs/lifecycle-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = kriLifecycleScheduler.runCurrentTenant(day);
        return Map.of(
                "asOf", day.toString(),
                "dueNotified", result.dueNotified(),
                "overdueNotified", result.overdueNotified());
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
