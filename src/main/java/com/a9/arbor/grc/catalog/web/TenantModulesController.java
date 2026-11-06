package com.a9.arbor.grc.catalog.web;

import com.a9.arbor.grc.catalog.service.LicenseBanners;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.catalog.web.dto.LicenseBannerResponse;
import com.a9.arbor.grc.catalog.web.dto.ModuleLicenseResponse;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.license.service.LicenseBannerService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenant-facing module licence lookup (JWT tenant). Used by the SPA to filter navigation
 * and to show the licence expiry banner.
 */
@RestController
@RequestMapping("/api/modules")
public class TenantModulesController {

    private final ModuleLicensingService moduleLicensingService;
    private final LicenseBannerService licenseBannerService;
    private final UserService userService;
    private final AdminModuleAccessService adminModuleAccessService;

    public TenantModulesController(ModuleLicensingService moduleLicensingService,
                                   LicenseBannerService licenseBannerService,
                                   UserService userService,
                                   AdminModuleAccessService adminModuleAccessService) {
        this.moduleLicensingService = moduleLicensingService;
        this.licenseBannerService = licenseBannerService;
        this.userService = userService;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    @GetMapping
    public Map<String, Object> list(Authentication authentication) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context required");
        }
        List<ModuleLicenseResponse> modules = moduleLicensingService.listForTenant(tenantId);
        java.util.Set<String> visible = adminModuleAccessService.visibleModuleCodes();
        if (visible != null) {
            modules = modules.stream().filter(m -> visible.contains(m.code())).toList();
        }
        List<String> enabled = modules.stream()
                .filter(ModuleLicenseResponse::enabled)
                .map(ModuleLicenseResponse::code)
                .toList();
        boolean writeLocked = LicenseBanners.writeLocked(modules);
        List<String> expiredCodes = modules.stream()
                .filter(m -> m.enabled() && m.expired())
                .map(ModuleLicenseResponse::code)
                .sorted()
                .toList();
        Long userId = currentUserId(authentication);
        LicenseBannerResponse bannerDraft = LicenseBanners.from(modules, false);
        LicenseBannerResponse banner = null;
        if (bannerDraft != null) {
            boolean dismissed = userId != null && licenseBannerService.isDismissed(userId, bannerDraft.key());
            banner = new LicenseBannerResponse(
                    bannerDraft.key(), bannerDraft.tone(), bannerDraft.title(), bannerDraft.message(), dismissed);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", tenantId);
        body.put("modules", modules);
        body.put("enabled", enabled);
        body.put("expiredCodes", expiredCodes);
        body.put("writeLocked", writeLocked);
        body.put("banner", banner);
        return body;
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return userService.findByEmail(authentication.getName()).map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
