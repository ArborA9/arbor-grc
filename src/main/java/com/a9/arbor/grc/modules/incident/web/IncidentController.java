package com.a9.arbor.grc.modules.incident.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.incident.service.IncidentLifecycleScheduler;
import com.a9.arbor.grc.modules.incident.service.IncidentService;
import com.a9.arbor.grc.modules.incident.web.dto.AssignIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.CreateIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.IncidentResponse;
import com.a9.arbor.grc.modules.incident.web.dto.ReportIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.ResolveIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.UpdateIncidentRequest;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiresModule({ModuleCode.ERM})
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentLifecycleScheduler incidentLifecycleScheduler;
    private final UserService userService;

    public IncidentController(IncidentService incidentService,
                              IncidentLifecycleScheduler incidentLifecycleScheduler,
                              UserService userService) {
        this.incidentService = incidentService;
        this.incidentLifecycleScheduler = incidentLifecycleScheduler;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<IncidentResponse> list(Authentication authentication) {
        return incidentService.list(currentUser(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public IncidentResponse get(Authentication authentication, @PathVariable long id) {
        return incidentService.get(currentUser(authentication), id);
    }

    @PostMapping("/report")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public IncidentResponse report(Authentication authentication,
                                   @Valid @RequestBody ReportIncidentRequest request) {
        return incidentService.report(currentUser(authentication), request);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public IncidentResponse create(Authentication authentication,
                                   @Valid @RequestBody CreateIncidentRequest request) {
        return incidentService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public IncidentResponse update(Authentication authentication,
                                   @PathVariable long id,
                                   @Valid @RequestBody UpdateIncidentRequest request) {
        return incidentService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public IncidentResponse assign(Authentication authentication,
                                   @PathVariable long id,
                                   @Valid @RequestBody AssignIncidentRequest request) {
        return incidentService.assign(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public IncidentResponse resolve(Authentication authentication,
                                    @PathVariable long id,
                                    @RequestBody(required = false) ResolveIncidentRequest request) {
        return incidentService.resolve(currentUser(authentication), id,
                request != null ? request : new ResolveIncidentRequest(null, null, null));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public IncidentResponse close(Authentication authentication, @PathVariable long id) {
        return incidentService.close(adminUser(authentication), id);
    }

    @PostMapping("/jobs/lifecycle-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        adminUser(authentication);
        Instant when = asOf == null || asOf.isBlank() ? Instant.now() : Instant.parse(asOf);
        var result = incidentLifecycleScheduler.runCurrentTenant(when);
        return Map.of("asOf", when.toString(), "overdueNotified", result.overdueNotified());
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
