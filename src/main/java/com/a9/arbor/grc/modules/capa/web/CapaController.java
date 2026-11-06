package com.a9.arbor.grc.modules.capa.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.capa.domain.CapaRegister;
import com.a9.arbor.grc.modules.capa.service.CapaLifecycleScheduler;
import com.a9.arbor.grc.modules.capa.service.CapaService;
import com.a9.arbor.grc.modules.capa.web.dto.AssignCapaRequest;
import com.a9.arbor.grc.modules.capa.web.dto.CapaResponse;
import com.a9.arbor.grc.modules.capa.web.dto.CreateCapaRequest;
import com.a9.arbor.grc.modules.capa.web.dto.RejectCapaRequest;
import com.a9.arbor.grc.modules.capa.web.dto.SubmitCapaResponseRequest;
import com.a9.arbor.grc.modules.capa.web.dto.UpdateCapaProgressRequest;
import com.a9.arbor.grc.modules.capa.web.dto.UpdateCapaTrackerRequest;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
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
@RequestMapping("/api/capa")
@RequiresModule({ModuleCode.ISMS, ModuleCode.AUDIT, ModuleCode.BCMS, ModuleCode.ERM})
public class CapaController {

    private final CapaService capaService;
    private final CapaLifecycleScheduler capaLifecycleScheduler;
    private final UserService userService;

    public CapaController(CapaService capaService,
                          CapaLifecycleScheduler capaLifecycleScheduler,
                          UserService userService) {
        this.capaService = capaService;
        this.capaLifecycleScheduler = capaLifecycleScheduler;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER','CONTRIBUTOR')")
    public List<CapaResponse> list(Authentication authentication,
                                   @RequestParam(required = false) CapaRegister register,
                                   @RequestParam(required = false) Long capaRegisterId) {
        return capaService.list(current(authentication), register, capaRegisterId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','GLOBAL_READER','CONTRIBUTOR')")
    public CapaResponse get(Authentication authentication, @PathVariable long id) {
        return capaService.get(current(authentication), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CapaResponse create(Authentication authentication, @Valid @RequestBody CreateCapaRequest request) {
        return capaService.createManual(current(authentication), request);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CapaResponse assign(Authentication authentication,
                               @PathVariable long id,
                               @Valid @RequestBody AssignCapaRequest request) {
        return capaService.assign(current(authentication), id, request);
    }

    @PutMapping("/{id}/tracker")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CapaResponse tracker(Authentication authentication,
                                @PathVariable long id,
                                @RequestBody UpdateCapaTrackerRequest request) {
        return capaService.updateTracker(current(authentication), id, request);
    }

    @PutMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public CapaResponse progress(Authentication authentication,
                                 @PathVariable long id,
                                 @RequestBody UpdateCapaProgressRequest request) {
        return capaService.updateProgress(current(authentication), id, request);
    }

    /** Excel cols O-P: Contributor submits their response description and evidence note. */
    @PutMapping("/{id}/response")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public CapaResponse contributorResponse(Authentication authentication,
                                            @PathVariable long id,
                                            @RequestBody SubmitCapaResponseRequest request) {
        return capaService.submitContributorResponse(current(authentication), id, request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public CapaResponse submit(Authentication authentication, @PathVariable long id) {
        return capaService.submit(current(authentication), id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CapaResponse close(Authentication authentication, @PathVariable long id) {
        return capaService.close(current(authentication), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public CapaResponse reject(Authentication authentication,
                               @PathVariable long id,
                               @Valid @RequestBody RejectCapaRequest request) {
        return capaService.reject(current(authentication), id, request);
    }

    /**
     * Run CAPA due-soon / overdue / escalation jobs for this tenant (Path C A4).
     * Optional {@code asOf=YYYY-MM-DD} for tests / backdated runs.
     */
    @PostMapping("/jobs/lifecycle-run")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, Object> runLifecycle(Authentication authentication,
                                            @RequestParam(required = false) String asOf) {
        current(authentication);
        LocalDate day = asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf);
        var result = capaLifecycleScheduler.runCurrentTenant(day);
        return Map.of(
                "asOf", day.toString(),
                "dueSoonNotified", result.dueSoonNotified(),
                "overdueMarked", result.overdueMarked(),
                "escalations", result.escalations());
    }

    private User current(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
