package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrcAuditRepository extends JpaRepository<GrcAudit, Long> {
    List<GrcAudit> findAllByOrderByUpdatedAtDesc();

    List<GrcAudit> findByIdInOrderByUpdatedAtDesc(List<Long> ids);

    List<GrcAudit> findByLeadAuditorUserIdOrderByUpdatedAtDesc(Long leadAuditorUserId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
