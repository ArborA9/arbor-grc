package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditRefSeq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRefSeqRepository extends JpaRepository<AuditRefSeq, AuditRefSeq.Pk> {
}
