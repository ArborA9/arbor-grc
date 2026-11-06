package com.a9.arbor.grc.modules.privacy.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.privacy.service.PrivacyInventoryService;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateInventoryRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.InventoryResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateInventoryRequest;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/privacy/inventory")
@RequiresModule(ModuleCode.PRIVACY)
public class PrivacyInventoryController {

    private static final String READ_ROLES =
            "hasAnyRole('OWNER','ADMIN','GLOBAL_READER','INTERNAL_AUDITOR','EXTERNAL_AUDITOR','CONTRIBUTOR')";

    private final PrivacyInventoryService inventoryService;
    private final UserService userService;

    public PrivacyInventoryController(PrivacyInventoryService inventoryService, UserService userService) {
        this.inventoryService = inventoryService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<InventoryResponse> list(Authentication authentication) {
        return inventoryService.list(viewer(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public InventoryResponse get(Authentication authentication, @PathVariable long id) {
        return inventoryService.get(viewer(authentication), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public InventoryResponse create(Authentication authentication,
                                    @Valid @RequestBody CreateInventoryRequest request) {
        return inventoryService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public InventoryResponse update(Authentication authentication,
                                    @PathVariable long id,
                                    @Valid @RequestBody UpdateInventoryRequest request) {
        return inventoryService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public InventoryResponse archive(Authentication authentication, @PathVariable long id) {
        return inventoryService.archive(adminUser(authentication), id);
    }

    private User adminUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required"));
    }

    private User viewer(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
    }
}
