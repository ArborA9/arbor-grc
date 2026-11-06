package com.a9.arbor.grc.modules.workflow.web;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.workflow.service.WorkflowService;
import com.a9.arbor.grc.modules.workflow.web.dto.StartWorkflowRequest;
import com.a9.arbor.grc.modules.workflow.web.dto.TransitionWorkflowRequest;
import com.a9.arbor.grc.modules.workflow.web.dto.WorkflowDefinitionResponse;
import com.a9.arbor.grc.modules.workflow.web.dto.WorkflowInstanceResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

/**
 * Central workflow engine (approvals). Platform always-on. Vendors have no access.
 * Contributor-only users use My Tasks: not the demo workflow console (Layer-3).
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final UserService userService;

    public WorkflowController(WorkflowService workflowService, UserService userService) {
        this.workflowService = workflowService;
        this.userService = userService;
    }

    @GetMapping("/definitions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<WorkflowDefinitionResponse> definitions() {
        return workflowService.listDefinitions();
    }

    @GetMapping("/instances")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public Page<WorkflowInstanceResponse> instances(Authentication authentication,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        int safe = Math.min(Math.max(size, 1), 100);
        return workflowService.listInstances(status, elevatedUser(authentication), PageRequest.of(page, safe));
    }

    @GetMapping("/instances/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public WorkflowInstanceResponse get(Authentication authentication, @PathVariable long id) {
        return workflowService.getInstance(id, elevatedUser(authentication));
    }

    @PostMapping("/instances")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public WorkflowInstanceResponse start(Authentication authentication,
                                          @Valid @RequestBody StartWorkflowRequest request) {
        return workflowService.start(elevatedUser(authentication), request);
    }

    @PostMapping("/instances/{id}/transitions")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public WorkflowInstanceResponse transition(Authentication authentication,
                                               @PathVariable long id,
                                               @Valid @RequestBody TransitionWorkflowRequest request) {
        return workflowService.transition(elevatedUser(authentication), id, request);
    }

    private User elevatedUser(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().noneMatch(r -> r == Role.VENDOR))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No workflow access"));
        if (RoleAccess.isContributorOnly(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Contributors use My Tasks: workflow console is not available");
        }
        return user;
    }
}
