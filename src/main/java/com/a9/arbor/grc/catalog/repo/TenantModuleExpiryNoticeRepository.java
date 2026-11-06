package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.LicenseExpiryMilestone;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.TenantModuleExpiryNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TenantModuleExpiryNoticeRepository extends JpaRepository<TenantModuleExpiryNotice, Long> {

    boolean existsByTenantIdAndModuleCodeAndMilestoneAndExpiresOn(
            String tenantId, ModuleCode moduleCode, LicenseExpiryMilestone milestone, LocalDate expiresOn);

    List<TenantModuleExpiryNotice> findByTenantIdAndModuleCode(String tenantId, ModuleCode moduleCode);
}
