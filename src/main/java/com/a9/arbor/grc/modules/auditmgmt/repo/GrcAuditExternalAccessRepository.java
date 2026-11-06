package com.a9.arbor.grc.modules.auditmgmt.repo;

import com.a9.arbor.grc.modules.auditmgmt.domain.ExternalAccessStatus;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditExternalAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrcAuditExternalAccessRepository extends JpaRepository<GrcAuditExternalAccess, Long> {

    Optional<GrcAuditExternalAccess> findByTokenHash(String tokenHash);

    boolean existsByAuditIdAndUserIdAndStatus(Long auditId, Long userId, ExternalAccessStatus status);

    List<GrcAuditExternalAccess> findByAuditIdOrderByCreatedAtDesc(Long auditId);

    List<GrcAuditExternalAccess> findByUserIdAndStatus(Long userId, ExternalAccessStatus status);

    Optional<GrcAuditExternalAccess> findFirstByUserIdAndStatusOrderByRedeemedAtDesc(
            Long userId, ExternalAccessStatus status);

    Optional<GrcAuditExternalAccess> findFirstByUserIdAndAuditIdAndStatusOrderByRedeemedAtDesc(
            Long userId, Long auditId, ExternalAccessStatus status);
}
