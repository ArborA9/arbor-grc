package com.a9.arbor.grc.modules.evidence.repo;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvidenceLinkRepository extends JpaRepository<EvidenceLink, Long> {

    List<EvidenceLink> findByEvidenceIdOrderByLinkedAtDesc(Long evidenceId);

    Optional<EvidenceLink> findByIdAndEvidenceId(Long id, Long evidenceId);

    boolean existsByEvidenceId(Long evidenceId);

    List<EvidenceLink> findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
            String moduleCode, String recordType, String recordId);

    List<EvidenceLink> findByModuleCodeAndRecordTypeAndRecordIdStartingWithOrderByLinkedAtDesc(
            String moduleCode, String recordType, String recordIdPrefix);

    @Query("""
            SELECT COUNT(l) FROM EvidenceLink l
            WHERE l.moduleCode = :moduleCode
              AND l.recordType = :recordType
              AND l.recordId = :recordId
              AND l.evidenceId IN (
                SELECT e.id FROM Evidence e
                WHERE e.deletedAt IS NULL
                  AND e.createdByUserId = :userId
              )
            """)
    long countActiveByUploaderAndRecord(@Param("userId") Long userId,
                                        @Param("moduleCode") String moduleCode,
                                        @Param("recordType") String recordType,
                                        @Param("recordId") String recordId);
}
