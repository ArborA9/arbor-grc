package com.a9.arbor.grc.modules.iam.web;

import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.SsoGroupRoleMappingService;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.iam.web.dto.CreateSsoGroupRoleMappingRequest;
import com.a9.arbor.grc.modules.iam.web.dto.SsoGroupRoleMappingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Tenant-managed IdP group → Arbor role mappings (Path C B2). Applied as a union on every SSO
 * login; never removes Admin-assigned roles.
 */
@RestController
@RequestMapping("/api/sso/group-role-mappings")
public class SsoGroupRoleMappingController {

    private final SsoGroupRoleMappingService service;
    private final UserService userService;

    public SsoGroupRoleMappingController(SsoGroupRoleMappingService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<SsoGroupRoleMappingResponse> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<SsoGroupRoleMappingResponse> create(Authentication authentication,
                                                              @Valid @RequestBody CreateSsoGroupRoleMappingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(actor(authentication), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void delete(Authentication authentication, @PathVariable long id) {
        service.delete(actor(authentication), id);
    }

    private User actor(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown actor"));
    }
}
