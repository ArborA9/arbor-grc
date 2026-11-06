package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditScopeNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrcAuditScopeNodeRepository extends JpaRepository<GrcAuditScopeNode, GrcAuditScopeNode.Pk> {

    List<GrcAuditScopeNode> findByAuditId(Long auditId);

    void deleteByAuditId(Long auditId);
}
