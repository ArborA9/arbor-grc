package com.a9.arbor.grc.modules.bcms.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.bcms.service.RecoveryPlanService;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateRecoveryPlanRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.RecoveryPlanResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.UpdateRecoveryPlanRequest;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.security.RequiresModule;
import jakarta.validation.Valid;
import com.a9.arbor.grc.common.web.ContentDispositionSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/bcms/recovery-plans")
@RequiresModule(ModuleCode.BCMS)
public class RecoveryPlanController {

    private final RecoveryPlanService recoveryPlanService;
    private final UserService userService;

    public RecoveryPlanController(RecoveryPlanService recoveryPlanService, UserService userService) {
        this.recoveryPlanService = recoveryPlanService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public List<RecoveryPlanResponse> list(@RequestParam(required = false) Long projectId) {
        return recoveryPlanService.list(projectId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public RecoveryPlanResponse get(@PathVariable long id) {
        return recoveryPlanService.get(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')")
    public ResponseEntity<byte[]> pdf(@PathVariable long id) {
        var file = recoveryPlanService.pdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(file.filename()))
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RecoveryPlanResponse create(Authentication authentication,
                                       @Valid @RequestBody CreateRecoveryPlanRequest request) {
        return recoveryPlanService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RecoveryPlanResponse update(Authentication authentication,
                                       @PathVariable long id,
                                       @Valid @RequestBody UpdateRecoveryPlanRequest request) {
        return recoveryPlanService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public RecoveryPlanResponse submit(Authentication authentication, @PathVariable long id) {
        return recoveryPlanService.submit(adminUser(authentication), id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR','INTERNAL_AUDITOR')")
    public RecoveryPlanResponse approve(Authentication authentication, @PathVariable long id) {
        return recoveryPlanService.approve(currentUser(authentication), id);
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
