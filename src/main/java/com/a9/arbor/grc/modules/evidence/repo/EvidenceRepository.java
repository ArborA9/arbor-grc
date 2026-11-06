package com.a9.arbor.grc.modules.evidence.repo;

import com.a9.arbor.grc.modules.evidence.domain.Evidence;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceClassification;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    Optional<Evidence> findByIdAndDeletedAtIsNull(Long id);

    List<Evidence> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    @Query("""
            SELECT e FROM Evidence e
            WHERE e.deletedAt IS NULL
              AND e.status = com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus.ACTIVE
              AND e.expiresOn IS NOT NULL
              AND e.expiresOn < :asOf
            """)
    List<Evidence> findActivePastExpiry(@Param("asOf") LocalDate asOf);

    /** Dashboard widget: active evidence already expired OR expiring within the window (irrespective of notify flag). */
    @Query("""
            SELECT COUNT(e) FROM Evidence e
            WHERE e.deletedAt IS NULL
              AND e.status = com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus.ACTIVE
              AND e.expiresOn IS NOT NULL
              AND e.expiresOn <= :windowEnd
            """)
    long countActiveExpiringOrExpired(@Param("windowEnd") LocalDate windowEnd);

    /** Report: active evidence already expired OR expiring by windowEnd. */
    @Query("""
            SELECT e FROM Evidence e
            WHERE e.deletedAt IS NULL
              AND e.status = com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus.ACTIVE
              AND e.expiresOn IS NOT NULL
              AND e.expiresOn <= :windowEnd
            ORDER BY e.expiresOn ASC
            """)
    List<Evidence> findActiveExpiringOrExpired(@Param("windowEnd") LocalDate windowEnd);

    @Query("""
            SELECT e FROM Evidence e
            WHERE e.deletedAt IS NULL
              AND e.status = com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus.ACTIVE
              AND e.expiresOn IS NOT NULL
              AND e.expiresOn >= :asOf
              AND e.expiresOn <= :windowEnd
              AND e.expiryNotifiedAt IS NULL
            """)
    List<Evidence> findExpiringForNotify(@Param("asOf") LocalDate asOf,
                                         @Param("windowEnd") LocalDate windowEnd);

    @Query("""
            SELECT e FROM Evidence e
            WHERE e.deletedAt IS NULL
              AND e.classification IN :allowed
              AND (:status IS NULL OR e.status = :status)
              AND (:classification IS NULL OR e.classification = :classification)
              AND (:q IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Evidence> search(@Param("q") String q,
                          @Param("classification") EvidenceClassification classification,
                          @Param("status") EvidenceStatus status,
                          @Param("allowed") Collection<EvidenceClassification> allowed,
                          Pageable pageable);

    @Query("""
            SELECT e FROM Evidence e
            WHERE e.deletedAt IS NULL
              AND e.classification IN :allowed
              AND (:status IS NULL OR e.status = :status)
              AND (:classification IS NULL OR e.classification = :classification)
              AND (:q IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (
                e.createdByUserId = :userId
                OR e.id IN (
                  SELECT l.evidenceId FROM EvidenceLink l
                  WHERE (l.moduleCode = 'GAP' AND l.recordType = 'GAP_ITEM'
                         AND l.recordId IN :gapItemIds)
                     OR (l.moduleCode = 'GAP' AND l.recordType = 'GAP_QUESTION'
                         AND LOCATE(':', l.recordId) > 0
                         AND SUBSTRING(l.recordId, 1, LOCATE(':', l.recordId) - 1) IN :gapItemIds)
                     OR (l.moduleCode = 'RISK' AND l.recordType = 'RISK_RECORD'
                         AND l.recordId IN :riskIds)
                     OR (l.moduleCode = 'CAPA' AND l.recordType = 'CAPA'
                         AND l.recordId IN :capaIds)
                )
              )
            """)
    Page<Evidence> searchForContributor(@Param("q") String q,
                                        @Param("classification") EvidenceClassification classification,
                                        @Param("status") EvidenceStatus status,
                                        @Param("allowed") Collection<EvidenceClassification> allowed,
                                        @Param("userId") Long userId,
                                        @Param("gapItemIds") Collection<String> gapItemIds,
                                        @Param("riskIds") Collection<String> riskIds,
                                        @Param("capaIds") Collection<String> capaIds,
                                        Pageable pageable);
}
