package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditScopeModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GrcAuditScopeModuleRepository extends JpaRepository<GrcAuditScopeModule, GrcAuditScopeModule.Pk> {

    List<GrcAuditScopeModule> findByAuditId(Long auditId);

    List<GrcAuditScopeModule> findByAuditIdIn(Collection<Long> auditIds);

    void deleteByAuditId(Long auditId);
}
