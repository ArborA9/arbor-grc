package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.TenantFramework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantFrameworkRepository extends JpaRepository<TenantFramework, Long> {

    List<TenantFramework> findByTenantIdOrderByActivatedAtDesc(String tenantId);

    Optional<TenantFramework> findByTenantIdAndFrameworkId(String tenantId, Long frameworkId);

    boolean existsByTenantIdAndFrameworkId(String tenantId, Long frameworkId);

    void deleteByTenantIdAndFrameworkId(String tenantId, Long frameworkId);

    boolean existsByFrameworkId(Long frameworkId);

    List<TenantFramework> findByFrameworkId(Long frameworkId);
}
