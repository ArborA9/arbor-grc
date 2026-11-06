package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkEntitlements;
import com.a9.arbor.grc.catalog.domain.FrameworkStatus;
import com.a9.arbor.grc.catalog.domain.IncludedModules;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.TenantFrameworkEntitlement;
import com.a9.arbor.grc.catalog.domain.TenantModule;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.repo.TenantFrameworkEntitlementRepository;
import com.a9.arbor.grc.catalog.repo.TenantModuleRepository;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.catalog.domain.LicenseDates;
import com.a9.arbor.grc.catalog.web.dto.ExtendModulesExpiryRequest;
import com.a9.arbor.grc.catalog.web.dto.ModuleLicenseResponse;
import com.a9.arbor.grc.catalog.web.dto.TenantLicenseSummary;
import com.a9.arbor.grc.catalog.web.dto.UpdateModulesRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catalog-level module licensing + per-framework entitlements (Doc 02 T-02 / T-13).
 */
@Service
public class ModuleLicensingService {

    private final TenantModuleRepository moduleRepository;
    private final TenantRepository tenantRepository;
    private final FrameworkRepository frameworkRepository;
    private final TenantFrameworkEntitlementRepository entitlementRepository;

    public ModuleLicensingService(TenantModuleRepository moduleRepository,
                                  TenantRepository tenantRepository,
                                  FrameworkRepository frameworkRepository,
                                  TenantFrameworkEntitlementRepository entitlementRepository) {
        this.moduleRepository = moduleRepository;
        this.tenantRepository = tenantRepository;
        this.frameworkRepository = frameworkRepository;
        this.entitlementRepository = entitlementRepository;
    }

    /** Creates licence rows for every module code (idempotent). AUDIT is always activated (Doc 02 T-13). */
    @Transactional(transactionManager = "catalogTransactionManager")
    public void ensureDefaults(String tenantId) {
        requireTenant(tenantId);
        for (ModuleCode code : ModuleCode.values()) {
            moduleRepository.findByTenantIdAndModuleCode(tenantId, code).orElseGet(() -> {
                TenantModule row = new TenantModule();
                row.setTenantId(tenantId);
                row.setModuleCode(code);
                row.setEnabled(code == ModuleCode.AUDIT);
                if (code == ModuleCode.AUDIT) {
                    row.setEnabledAt(Instant.now());
                }
                return moduleRepository.save(row);
            });
        }
        moduleRepository.findByTenantIdAndModuleCode(tenantId, ModuleCode.AUDIT).ifPresent(row -> {
            if (!row.isEnabled()) {
                row.setEnabled(true);
                row.setEnabledAt(Instant.now());
                moduleRepository.save(row);
            }
        });
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public boolean isEnabled(String tenantId, ModuleCode code) {
        if (code == null || !code.delivered()) {
            return false;
        }
        if (IncludedModules.isIncluded(code)) {
            return effectiveRow(tenantId, code).map(TenantModule::isEnabled).orElse(false);
        }
        return moduleRepository.existsByTenantIdAndModuleCodeAndEnabledTrue(tenantId, code);
    }

    /** Licensed module whose last inclusive day is in the past. */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public boolean isModuleExpired(String tenantId, ModuleCode code) {
        if (tenantId == null || tenantId.isBlank() || code == null) {
            return false;
        }
        LocalDate today = LocalDate.now(LicenseDates.ZONE);
        java.util.Optional<TenantModule> licence = IncludedModules.isIncluded(code)
                ? effectiveRow(tenantId, code)
                : moduleRepository.findByTenantIdAndModuleCode(tenantId, code);
        return licence
                .filter(TenantModule::isEnabled)
                .map(row -> LicenseDates.isExpired(row.getExpiresAt(), today))
                .orElse(false);
    }

    /** Licensed and still inside the last inclusive day. */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public boolean isWritable(String tenantId, ModuleCode code) {
        return isEnabled(tenantId, code) && !isModuleExpired(tenantId, code);
    }

    /**
     * True when any licensed module has passed its expiry date (Super Admin KPI / mixed-term flag).
     * Does not mean the whole workspace is locked.
     */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public boolean isWriteLocked(String tenantId) {
        return !expiredCodes(tenantId).isEmpty();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<String> expiredCodes(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(LicenseDates.ZONE);
        List<String> codes = new ArrayList<>();
        for (TenantModule row : effectiveRows(tenantId)) {
            if (row.getModuleCode() == null || !row.getModuleCode().delivered()) {
                continue;
            }
            if (row.isEnabled() && LicenseDates.isExpired(row.getExpiresAt(), today)) {
                codes.add(row.getModuleCode().name());
            }
        }
        return List.copyOf(codes);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public void requireWritable(String tenantId, ModuleCode code) {
        if (tenantId == null || tenantId.isBlank() || code == null || !isEnabled(tenantId, code)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    LicenseExpiryCopy.notLicensed(code == null ? "" : code.name()));
        }
        if (isModuleExpired(tenantId, code)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, LicenseExpiryCopy.writeForbidden(code.name()));
        }
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public void requireWritableFramework(String tenantId, Long frameworkId) {
        Framework framework = frameworkRepository.findById(frameworkId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found"));
        requireWritable(tenantId, framework.getModuleCode());
    }

    /** Compact licence roll-up for Super Admin tenant list (one catalog query). */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public Map<String, TenantLicenseSummary> summariesForTenants(Collection<String> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Map.of();
        }
        List<String> ids = tenantIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now(LicenseDates.ZONE);
        Map<String, List<TenantModule>> byTenant = new HashMap<>();
        for (TenantModule row : moduleRepository.findByTenantIdIn(ids)) {
            byTenant.computeIfAbsent(row.getTenantId(), k -> new ArrayList<>()).add(row);
        }
        Map<String, TenantLicenseSummary> out = new LinkedHashMap<>();
        for (String id : ids) {
            out.put(id, TenantLicenseSummary.from(
                    IncludedModules.effective(byTenant.getOrDefault(id, List.of())), today));
        }
        return out;
    }

    /** True if the tenant may activate/see this published framework (module on + entitled). */
    @Transactional(transactionManager = "catalogTransactionManager")
    public boolean isFrameworkEntitled(String tenantId, Long frameworkId) {
        Framework framework = frameworkRepository.findById(frameworkId).orElse(null);
        if (framework == null || framework.getModuleCode() == null) {
            return false;
        }
        if (!isEnabled(tenantId, framework.getModuleCode())) {
            return false;
        }
        return entitlementRepository.existsByTenantIdAndFrameworkId(tenantId, frameworkId);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public List<ModuleLicenseResponse> listForTenant(String tenantId) {
        ensureDefaults(tenantId);
        return toResponses(tenantId);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public List<ModuleLicenseResponse> updateForTenant(String tenantId, UpdateModulesRequest request) {
        return updateForTenant(tenantId, request, null);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public List<ModuleLicenseResponse> updateForTenant(String tenantId, UpdateModulesRequest request,
                                                       String actorEmail) {
        ensureDefaults(tenantId);
        Map<ModuleCode, TenantModule> byCode = new EnumMap<>(ModuleCode.class);
        for (TenantModule row : moduleRepository.findByTenantIdOrderByModuleCodeAsc(tenantId)) {
            byCode.put(row.getModuleCode(), row);
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(LicenseDates.ZONE);
        LocalDate sharedExpiry = request.expiresOn();
        for (UpdateModulesRequest.ModuleToggle toggle : request.modules()) {
            ModuleCode code = parseCode(toggle.code());
            if (!code.delivered()) {
                continue;
            }
            TenantModule row = byCode.get(code);
            if (row == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown module: " + code);
            }
            boolean enable = Boolean.TRUE.equals(toggle.enabled());
            if (code == ModuleCode.AUDIT) {
                enable = true;
            }
            if (enable) {
                boolean newlyLicensed = !row.isEnabled();
                if (newlyLicensed) {
                    row.setEnabled(true);
                    row.setEnabledAt(now);
                }
                LocalDate expiresOn = toggle.expiresOn() != null ? toggle.expiresOn() : sharedExpiry;
                if (IncludedModules.isIncluded(code)) {
                    row.setExpiresAt(null);
                } else if (expiresOn != null) {
                    applyExpiry(row, expiresOn, today);
                } else if (newlyLicensed && row.getExpiresAt() == null) {
                    applyExpiry(row, LicenseDates.defaultExpiryOn(today), today);
                }
                moduleRepository.save(row);
                replaceEntitlements(tenantId, code, toggle.frameworkIds(), actorEmail, now);
            } else if (row.isEnabled()) {
                row.setEnabled(false);
                row.setExpiresAt(null);
                moduleRepository.save(row);
                clearEntitlementsForModule(tenantId, code);
            }
        }
        return toResponses(tenantId);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public List<ModuleLicenseResponse> extendExpiry(String tenantId, ExtendModulesExpiryRequest request) {
        ensureDefaults(tenantId);
        LocalDate today = LocalDate.now(LicenseDates.ZONE);
        LocalDate expiresOn = request.expiresOn();
        try {
            LicenseDates.requireValidTerm(expiresOn, today);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        List<ModuleCode> targets = new ArrayList<>();
        if (request.codes() == null || request.codes().isEmpty()) {
            for (TenantModule row : moduleRepository.findByTenantIdOrderByModuleCodeAsc(tenantId)) {
                if (row.getModuleCode() != null && row.getModuleCode().delivered() && row.isEnabled()
                        && !IncludedModules.isIncluded(row.getModuleCode())) {
                    targets.add(row.getModuleCode());
                }
            }
        } else {
            for (String raw : request.codes()) {
                ModuleCode code = parseCode(raw);
                if (code.delivered() && !IncludedModules.isIncluded(code)) {
                    targets.add(code);
                }
            }
        }
        if (targets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No licensed modules to set expiry on");
        }
        Instant now = Instant.now();
        for (ModuleCode code : targets) {
            TenantModule row = moduleRepository.findByTenantIdAndModuleCode(tenantId, code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown module: " + code));
            if (!row.isEnabled()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Module " + code + " is not licensed for this tenant");
            }
            applyExpiry(row, expiresOn, today);
            if (row.getEnabledAt() == null) {
                row.setEnabledAt(now);
            }
            moduleRepository.save(row);
        }
        return toResponses(tenantId);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public void ensureFrameworkEntitled(String tenantId, Long frameworkId, String actorEmail) {
        Framework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found"));
        if (framework.getStatus() != FrameworkStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Framework is not published");
        }
        if (!isEnabled(tenantId, framework.getModuleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module " + framework.getModuleCode() + " is not licensed for this tenant");
        }
        if (entitlementRepository.existsByTenantIdAndFrameworkId(tenantId, frameworkId)) {
            return;
        }
        TenantFrameworkEntitlement row = new TenantFrameworkEntitlement();
        row.setTenantId(tenantId);
        row.setFrameworkId(frameworkId);
        row.setEntitledAt(Instant.now());
        row.setEntitledBy(actorEmail);
        entitlementRepository.save(row);
    }

    /** Enabled module codes only (for SPA nav filtering). */
    @Transactional(transactionManager = "catalogTransactionManager")
    public List<String> enabledCodes(String tenantId) {
        ensureDefaults(tenantId);
        List<String> enabled = new ArrayList<>();
        for (TenantModule row : effectiveRows(tenantId)) {
            if (row.getModuleCode() != null && row.getModuleCode().delivered() && row.isEnabled()) {
                enabled.add(row.getModuleCode().name());
            }
        }
        return enabled;
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public Set<Long> entitledFrameworkIds(String tenantId) {
        ensureDefaults(tenantId);
        return new HashSet<>(entitlementRepository.findFrameworkIdsByTenantId(tenantId));
    }

    private List<ModuleLicenseResponse> toResponses(String tenantId) {
        Map<ModuleCode, List<Long>> byModule = publishedByModule();
        Set<Long> entitled = new HashSet<>(entitlementRepository.findFrameworkIdsByTenantId(tenantId));
        LocalDate today = LocalDate.now(LicenseDates.ZONE);
        List<ModuleLicenseResponse> out = new ArrayList<>();
        for (TenantModule row : effectiveRows(tenantId)) {
            if (row.getModuleCode() == null || !row.getModuleCode().delivered()) {
                continue;
            }
            List<Long> published = byModule.getOrDefault(row.getModuleCode(), List.of());
            List<Long> ids = row.isEnabled()
                    ? published.stream().filter(entitled::contains).toList()
                    : List.of();
            out.add(ModuleLicenseResponse.from(row, ids, today));
        }
        return out;
    }

    private void applyExpiry(TenantModule row, LocalDate expiresOn, LocalDate today) {
        try {
            LicenseDates.requireValidTerm(expiresOn, today);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        row.setExpiresAt(LicenseDates.toExpiryInstant(expiresOn));
    }

    private void replaceEntitlements(String tenantId, ModuleCode code, List<Long> frameworkIds,
                                     String actorEmail, Instant now) {
        List<Framework> published = frameworkRepository
                .findByStatusAndModuleCodeInOrderByCodeAscVersionDesc(FrameworkStatus.PUBLISHED, List.of(code));
        // Prefer latest version per framework code when entitling "all" (avoids licensing every stub version).
        Map<String, Long> latestByCode = new java.util.LinkedHashMap<>();
        for (Framework f : published) {
            latestByCode.putIfAbsent(f.getCode(), f.getId());
        }
        Set<Long> publishedIds = published.stream().map(Framework::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> defaultAll = new LinkedHashSet<>(latestByCode.values());
        clearEntitlementsForModule(tenantId, code);

        Set<Long> selected;
        try {
            selected = FrameworkEntitlements.select(frameworkIds, publishedIds, defaultAll, code.name());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        for (Long id : selected) {
            TenantFrameworkEntitlement row = new TenantFrameworkEntitlement();
            row.setTenantId(tenantId);
            row.setFrameworkId(id);
            row.setEntitledAt(now);
            row.setEntitledBy(actorEmail);
            entitlementRepository.save(row);
        }
    }

    private void clearEntitlementsForModule(String tenantId, ModuleCode code) {
        List<Long> ids = frameworkRepository
                .findByStatusAndModuleCodeInOrderByCodeAscVersionDesc(FrameworkStatus.PUBLISHED, List.of(code))
                .stream()
                .map(Framework::getId)
                .toList();
        if (!ids.isEmpty()) {
            entitlementRepository.deleteByTenantIdAndFrameworkIdIn(tenantId, ids);
            entitlementRepository.flush();
        }
    }

    private Map<ModuleCode, List<Long>> publishedByModule() {
        Map<ModuleCode, List<Long>> map = new EnumMap<>(ModuleCode.class);
        for (Framework f : frameworkRepository.findByStatusAndModuleCodeInOrderByCodeAscVersionDesc(
                FrameworkStatus.PUBLISHED, List.of(ModuleCode.values()))) {
            map.computeIfAbsent(f.getModuleCode(), k -> new ArrayList<>()).add(f.getId());
        }
        return map;
    }

    private List<TenantModule> effectiveRows(String tenantId) {
        return IncludedModules.effective(moduleRepository.findByTenantIdOrderByModuleCodeAsc(tenantId));
    }

    private java.util.Optional<TenantModule> effectiveRow(String tenantId, ModuleCode code) {
        return effectiveRows(tenantId).stream().filter(r -> r.getModuleCode() == code).findFirst();
    }

    private void requireTenant(String tenantId) {
        if (!tenantRepository.existsByTenantId(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown tenant: " + tenantId);
        }
    }

    private static ModuleCode parseCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "module code is required");
        }
        try {
            return ModuleCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown module code: " + raw);
        }
    }
}
