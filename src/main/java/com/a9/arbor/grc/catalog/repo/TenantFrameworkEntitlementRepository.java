package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.TenantFrameworkEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TenantFrameworkEntitlementRepository extends JpaRepository<TenantFrameworkEntitlement, Long> {

    List<TenantFrameworkEntitlement> findByTenantId(String tenantId);

    boolean existsByTenantIdAndFrameworkId(String tenantId, Long frameworkId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TenantFrameworkEntitlement e where e.tenantId = :tenantId and e.frameworkId in :frameworkIds")
    void deleteByTenantIdAndFrameworkIdIn(@Param("tenantId") String tenantId,
                                          @Param("frameworkIds") Collection<Long> frameworkIds);

    @Query("select e.frameworkId from TenantFrameworkEntitlement e where e.tenantId = :tenantId")
    List<Long> findFrameworkIdsByTenantId(@Param("tenantId") String tenantId);
}
