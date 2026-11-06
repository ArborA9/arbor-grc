package com.a9.arbor.grc.modules.task.web;

import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.a9.arbor.grc.modules.task.web.dto.SaveGapDraftRequest;
import com.a9.arbor.grc.modules.task.web.dto.SubmitCapaTaskRequest;
import com.a9.arbor.grc.modules.task.web.dto.SubmitGapTaskRequest;
import com.a9.arbor.grc.modules.task.web.dto.SubmitMilestoneTaskRequest;
import com.a9.arbor.grc.modules.task.web.dto.SubmitRiskTaskRequest;
import com.a9.arbor.grc.modules.task.web.dto.WorkTaskDetailResponse;
import com.a9.arbor.grc.modules.task.web.dto.WorkTaskSummaryResponse;
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
@RequestMapping("/api/my-tasks")
public class MyTasksController {

    private final MyTasksService myTasksService;
    private final UserService userService;

    public MyTasksController(MyTasksService myTasksService, UserService userService) {
        this.myTasksService = myTasksService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public List<WorkTaskSummaryResponse> list(Authentication authentication) {
        return myTasksService.listMine(currentUser(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public WorkTaskDetailResponse get(Authentication authentication, @PathVariable long id) {
        return myTasksService.getMine(currentUser(authentication), id);
    }

    @PostMapping("/{id}/save-gap-draft")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse saveGapDraft(Authentication authentication,
                                               @PathVariable long id,
                                               @Valid @RequestBody SaveGapDraftRequest request) {
        return myTasksService.saveGapDraft(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-milestone")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitMilestone(Authentication authentication,
                                                  @PathVariable long id,
                                                  @Valid @RequestBody(required = false) SubmitMilestoneTaskRequest request) {
        return myTasksService.submitMilestoneTask(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-gap")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitGap(Authentication authentication,
                                            @PathVariable long id,
                                            @Valid @RequestBody SubmitGapTaskRequest request) {
        return myTasksService.submitGapTask(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-risk")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitRisk(Authentication authentication,
                                             @PathVariable long id,
                                             @Valid @RequestBody SubmitRiskTaskRequest request) {
        return myTasksService.submitRiskTask(currentUser(authentication), id, request.toSubmitRiskRequest());
    }

    @PostMapping("/{id}/submit-capa")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','INTERNAL_AUDITOR')")
    public WorkTaskDetailResponse submitCapa(Authentication authentication,
                                             @PathVariable long id,
                                             @Valid @RequestBody SubmitCapaTaskRequest request) {
        return myTasksService.submitCapaTask(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-bia")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitBia(Authentication authentication, @PathVariable long id) {
        return myTasksService.submitBiaTask(currentUser(authentication), id);
    }

    @PostMapping("/{id}/acknowledge-policy")
    @PreAuthorize("isAuthenticated() and !hasRole('VENDOR')")
    public WorkTaskDetailResponse acknowledgePolicy(Authentication authentication, @PathVariable long id) {
        return myTasksService.acknowledgePolicyTask(currentUser(authentication), id);
    }

    @PostMapping("/{id}/submit-kri")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitKri(Authentication authentication,
                                            @PathVariable long id,
                                            @Valid @RequestBody com.a9.arbor.grc.modules.kri.web.dto.RecordKriValueRequest request) {
        return myTasksService.submitKriTask(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-incident")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitIncident(Authentication authentication,
                                                 @PathVariable long id,
                                                 @Valid @RequestBody com.a9.arbor.grc.modules.incident.web.dto.SubmitIncidentTaskRequest request) {
        return myTasksService.submitIncidentTask(currentUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit-ropa")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitRopa(Authentication authentication, @PathVariable long id) {
        return myTasksService.submitRopaTask(currentUser(authentication), id);
    }

    @PostMapping("/{id}/submit-dpia")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public WorkTaskDetailResponse submitDpia(Authentication authentication, @PathVariable long id) {
        return myTasksService.submitDpiaTask(currentUser(authentication), id);
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
