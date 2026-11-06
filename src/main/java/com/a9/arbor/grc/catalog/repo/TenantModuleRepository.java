package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.TenantModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TenantModuleRepository extends JpaRepository<TenantModule, Long> {

    List<TenantModule> findByTenantIdOrderByModuleCodeAsc(String tenantId);

    List<TenantModule> findByTenantIdIn(Collection<String> tenantIds);

    Optional<TenantModule> findByTenantIdAndModuleCode(String tenantId, ModuleCode moduleCode);

    boolean existsByTenantIdAndModuleCodeAndEnabledTrue(String tenantId, ModuleCode moduleCode);
}
