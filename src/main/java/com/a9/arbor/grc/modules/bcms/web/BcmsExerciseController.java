package com.a9.arbor.grc.modules.bcms.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.bcms.service.ExerciseService;
import com.a9.arbor.grc.modules.bcms.web.dto.BcmsExerciseResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.CompleteExerciseRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateExerciseRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.UpdateExerciseRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/bcms/exercises")
@RequiresModule(ModuleCode.BCMS)
public class BcmsExerciseController {

    private final ExerciseService exerciseService;
    private final UserService userService;

    public BcmsExerciseController(ExerciseService exerciseService, UserService userService) {
        this.exerciseService = exerciseService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<BcmsExerciseResponse> list(@RequestParam(required = false) Long projectId) {
        return exerciseService.list(projectId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public BcmsExerciseResponse get(@PathVariable long id) {
        return exerciseService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BcmsExerciseResponse create(Authentication authentication,
                                       @Valid @RequestBody CreateExerciseRequest request) {
        return exerciseService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BcmsExerciseResponse update(Authentication authentication,
                                       @PathVariable long id,
                                       @Valid @RequestBody UpdateExerciseRequest request) {
        return exerciseService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BcmsExerciseResponse complete(Authentication authentication,
                                         @PathVariable long id,
                                         @Valid @RequestBody CompleteExerciseRequest request) {
        return exerciseService.complete(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public BcmsExerciseResponse cancel(Authentication authentication, @PathVariable long id) {
        return exerciseService.cancel(adminUser(authentication), id);
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }
}
