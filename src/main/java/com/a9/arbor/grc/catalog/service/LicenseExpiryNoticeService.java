package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.IncludedModules;
import com.a9.arbor.grc.catalog.domain.LicenseDates;
import com.a9.arbor.grc.catalog.domain.LicenseExpiryMilestone;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.domain.TenantModule;
import com.a9.arbor.grc.catalog.domain.TenantModuleExpiryNotice;
import com.a9.arbor.grc.catalog.domain.TenantStatus;
import com.a9.arbor.grc.catalog.repo.TenantModuleExpiryNoticeRepository;
import com.a9.arbor.grc.catalog.repo.TenantModuleRepository;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Catalog-side scan of licence expiry reminder windows. */
@Service
public class LicenseExpiryNoticeService {

    private final TenantRepository tenantRepository;
    private final TenantModuleRepository moduleRepository;
    private final TenantModuleExpiryNoticeRepository noticeRepository;

    public LicenseExpiryNoticeService(TenantRepository tenantRepository,
                                      TenantModuleRepository moduleRepository,
                                      TenantModuleExpiryNoticeRepository noticeRepository) {
        this.tenantRepository = tenantRepository;
        this.moduleRepository = moduleRepository;
        this.noticeRepository = noticeRepository;
    }

    public List<Tenant> activeTenants() {
        return tenantRepository.findByStatus(TenantStatus.ACTIVE);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<DueBatch> dueBatches(String tenantId, LocalDate asOf) {
        Map<LicenseExpiryMilestone, List<DueModule>> grouped = new EnumMap<>(LicenseExpiryMilestone.class);
        for (TenantModule row : moduleRepository.findByTenantIdOrderByModuleCodeAsc(tenantId)) {
            if (!row.isEnabled() || row.getExpiresAt() == null || IncludedModules.isIncluded(row.getModuleCode())) {
                continue;
            }
            LocalDate expiresOn = LicenseDates.toExpiryDate(row.getExpiresAt());
            int days = LicenseExpiryMilestone.daysRemaining(asOf, expiresOn);
            LicenseExpiryMilestone milestone = LicenseExpiryMilestone.matching(days).orElse(null);
            if (milestone == null) {
                continue;
            }
            if (noticeRepository.existsByTenantIdAndModuleCodeAndMilestoneAndExpiresOn(
                    tenantId, row.getModuleCode(), milestone, expiresOn)) {
                continue;
            }
            grouped.computeIfAbsent(milestone, k -> new ArrayList<>())
                    .add(new DueModule(row.getModuleCode(), expiresOn));
        }
        List<DueBatch> out = new ArrayList<>();
        for (LicenseExpiryMilestone milestone : LicenseExpiryMilestone.values()) {
            List<DueModule> modules = grouped.get(milestone);
            if (modules == null || modules.isEmpty()) {
                continue;
            }
            modules.sort(Comparator.comparing(DueModule::expiresOn).thenComparing(m -> m.code().name()));
            out.add(new DueBatch(milestone, List.copyOf(modules)));
        }
        return out;
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public void markSent(String tenantId, DueBatch batch) {
        Instant now = Instant.now();
        for (DueModule module : batch.modules()) {
            if (noticeRepository.existsByTenantIdAndModuleCodeAndMilestoneAndExpiresOn(
                    tenantId, module.code(), batch.milestone(), module.expiresOn())) {
                continue;
            }
            TenantModuleExpiryNotice row = new TenantModuleExpiryNotice();
            row.setTenantId(tenantId);
            row.setModuleCode(module.code());
            row.setMilestone(batch.milestone());
            row.setExpiresOn(module.expiresOn());
            row.setSentAt(now);
            noticeRepository.save(row);
        }
    }

    public record DueModule(ModuleCode code, LocalDate expiresOn) {
    }

    public record DueBatch(LicenseExpiryMilestone milestone, List<DueModule> modules) {
        public LocalDate earliestExpiry() {
            return modules.stream().map(DueModule::expiresOn).min(LocalDate::compareTo).orElse(null);
        }
    }
}
