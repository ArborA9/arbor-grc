package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditAccessLogRepository extends JpaRepository<AuditAccessLog, Long> {
    List<AuditAccessLog> findByGrantIdOrderByViewedAtDesc(Long grantId);

    List<AuditAccessLog> findAllByOrderByViewedAtDesc();

    long countByGrantId(Long grantId);
}
