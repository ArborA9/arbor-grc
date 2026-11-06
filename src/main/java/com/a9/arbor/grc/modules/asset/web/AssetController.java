package com.a9.arbor.grc.modules.asset.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.asset.service.AssetService;
import com.a9.arbor.grc.modules.asset.web.dto.AssetKpiResponse;
import com.a9.arbor.grc.modules.asset.web.dto.AssetMetaResponse;
import com.a9.arbor.grc.modules.asset.web.dto.AssetResponse;
import com.a9.arbor.grc.modules.asset.web.dto.CreateAssetRequest;
import com.a9.arbor.grc.modules.asset.web.dto.UpdateAssetRequest;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiresModule(ModuleCode.ISMS)
public class AssetController {

    private final AssetService assetService;
    private final UserService userService;

    public AssetController(AssetService assetService, UserService userService) {
        this.assetService = assetService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public List<AssetResponse> list(Authentication authentication) {
        return assetService.list(currentUser(authentication));
    }

    @GetMapping("/meta")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AssetMetaResponse meta() {
        return assetService.meta();
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AssetKpiResponse kpis(Authentication authentication) {
        return assetService.kpis(currentUser(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','GLOBAL_READER','CONTRIBUTOR','INTERNAL_AUDITOR','EXTERNAL_AUDITOR')")
    public AssetResponse get(Authentication authentication, @PathVariable long id) {
        return assetService.get(currentUser(authentication), id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssetResponse create(Authentication authentication, @Valid @RequestBody CreateAssetRequest request) {
        return assetService.create(adminUser(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssetResponse update(Authentication authentication,
                                @PathVariable long id,
                                @Valid @RequestBody UpdateAssetRequest request) {
        return assetService.update(adminUser(authentication), id, request);
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AssetResponse dispose(Authentication authentication, @PathVariable long id) {
        return assetService.dispose(adminUser(authentication), id);
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
