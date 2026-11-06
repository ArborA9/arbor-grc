package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditAccessGrantRepository extends JpaRepository<AuditAccessGrant, Long> {
    List<AuditAccessGrant> findByAuditIdOrderByIdAsc(Long auditId);

    List<AuditAccessGrant> findAllByOrderByIdDesc();

    List<AuditAccessGrant> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<AuditAccessGrant> findByAuditIdAndUserIdAndRevokedFalse(Long auditId, Long userId);

    boolean existsByAuditIdAndUserIdAndRevokedFalse(Long auditId, Long userId);
}
