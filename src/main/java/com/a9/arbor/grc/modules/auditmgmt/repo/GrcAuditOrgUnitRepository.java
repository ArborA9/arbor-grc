package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditOrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrcAuditOrgUnitRepository extends JpaRepository<GrcAuditOrgUnit, GrcAuditOrgUnit.Pk> {
    List<GrcAuditOrgUnit> findByAuditId(Long auditId);
    void deleteByAuditId(Long auditId);
}
