package com.a9.arbor.grc.catalog.web;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.catalog.service.TenantProvisioningService;
import com.a9.arbor.grc.catalog.web.dto.CreateTenantRequest;
import com.a9.arbor.grc.catalog.web.dto.ExtendModulesExpiryRequest;
import com.a9.arbor.grc.catalog.web.dto.ModuleLicenseResponse;
import com.a9.arbor.grc.catalog.web.dto.ProvisionTenantResponse;
import com.a9.arbor.grc.catalog.web.dto.TenantIdpConfigRequest;
import com.a9.arbor.grc.catalog.web.dto.TenantLicenseSummary;
import com.a9.arbor.grc.catalog.web.dto.TenantResponse;
import com.a9.arbor.grc.catalog.web.dto.UpdateModulesRequest;
import com.a9.arbor.grc.modules.license.service.LicenseExpiryScheduler;
import com.a9.arbor.grc.tenancy.TenantIds;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Catalog-level (Super Admin) tenant administration. These endpoints operate on the shared catalog
 * database and are exempt from tenant resolution.
 */
@RestController
@RequestMapping("/api/admin/tenants")
public class TenantAdminController {

    private final TenantProvisioningService provisioningService;
    private final ModuleLicensingService moduleLicensingService;
    private final LicenseExpiryScheduler licenseExpiryScheduler;

    public TenantAdminController(TenantProvisioningService provisioningService,
                                 ModuleLicensingService moduleLicensingService,
                                 LicenseExpiryScheduler licenseExpiryScheduler) {
        this.provisioningService = provisioningService;
        this.moduleLicensingService = moduleLicensingService;
        this.licenseExpiryScheduler = licenseExpiryScheduler;
    }

    @PostMapping
    public ResponseEntity<ProvisionTenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        ProvisionTenantResponse body = ProvisionTenantResponse.from(provisioningService.provision(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<TenantResponse> list() {
        List<Tenant> tenants = provisioningService.listTenants();
        List<String> ids = tenants.stream().map(Tenant::getTenantId).toList();
        Map<String, TenantLicenseSummary> licenses = moduleLicensingService.summariesForTenants(ids);
        return tenants.stream()
                .map(t -> TenantResponse.from(t, licenses.get(t.getTenantId())))
                .toList();
    }

    /** Configure a tenant's SSO / AD (OIDC) integration. Secret is stored, never returned. */
    @PutMapping("/{tenantId}/idp")
    public TenantResponse configureIdp(@PathVariable String tenantId,
                                       @Valid @RequestBody TenantIdpConfigRequest request) {
        return TenantResponse.from(provisioningService.configureIdp(tenantId, request));
    }

    /** New 24-hour activation email for Owners who have not set a password yet. */
    @PostMapping("/{tenantId}/owner-activation")
    public Map<String, Object> resendOwnerActivation(@PathVariable String tenantId) {
        String id = TenantIds.normalize(tenantId);
        return Map.of("tenantId", id, "sentTo", provisioningService.resendOwnerActivation(id));
    }

    /** List commercial module licences for a tenant (creates disabled defaults if missing). */
    @GetMapping("/{tenantId}/modules")
    public Map<String, Object> listModules(@PathVariable String tenantId) {
        String id = TenantIds.normalize(tenantId);
        List<ModuleLicenseResponse> modules = moduleLicensingService.listForTenant(id);
        return Map.of("tenantId", id, "modules", modules);
    }

    /** Enable/disable modules for a tenant. Unspecified codes are left unchanged. */
    @PutMapping("/{tenantId}/modules")
    public Map<String, Object> updateModules(@PathVariable String tenantId,
                                             @Valid @RequestBody UpdateModulesRequest request) {
        String id = TenantIds.normalize(tenantId);
        List<ModuleLicenseResponse> modules = moduleLicensingService.updateForTenant(id, request);
        return Map.of("tenantId", id, "modules", modules);
    }

    /** Set or extend expiry for one module, several, or every licensed module of a tenant. */
    @PutMapping("/{tenantId}/modules/expiry")
    public Map<String, Object> extendExpiry(@PathVariable String tenantId,
                                            @Valid @RequestBody ExtendModulesExpiryRequest request) {
        String id = TenantIds.normalize(tenantId);
        List<ModuleLicenseResponse> modules = moduleLicensingService.extendExpiry(id, request);
        return Map.of("tenantId", id, "modules", modules);
    }

    /** Ops / E2E: send due licence expiry notices for this tenant as of a calendar date. */
    @PostMapping("/{tenantId}/license-reminders/run")
    public Map<String, Object> runLicenseReminders(
            @PathVariable String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        String id = TenantIds.normalize(tenantId);
        LocalDate day = asOf == null ? LocalDate.now() : asOf;
        int sent = licenseExpiryScheduler.runTenant(id, day);
        return Map.of("tenantId", id, "asOf", day.toString(), "emailsQueued", sent);
    }
}

