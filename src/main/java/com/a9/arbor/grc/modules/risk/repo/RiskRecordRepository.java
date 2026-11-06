package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RiskRecordRepository extends JpaRepository<RiskRecord, Long> {
    List<RiskRecord> findByRiskTypeOrderByUpdatedAtDesc(RiskType riskType);

    List<RiskRecord> findByAssignedContributorIdOrderByUpdatedAtDesc(Long assignedContributorId);

    List<RiskRecord> findByRiskTypeAndAssignedContributorIdOrderByUpdatedAtDesc(
            RiskType riskType, Long assignedContributorId);

    @Query("""
            select distinct r from RiskRecord r
            left join RiskAcceptanceForm f on f.riskId = r.id
            where r.riskType = :type
              and (r.assignedContributorId = :userId
                   or r.riskOwnerUserId = :userId
                   or ((r.status = com.a9.arbor.grc.modules.risk.domain.RiskStatus.PENDING_ACCEPTANCE
                        or r.status = com.a9.arbor.grc.modules.risk.domain.RiskStatus.PENDING_ADMIN_REVIEW)
                       and (f.approver1UserId = :userId
                            or f.approver2UserId = :userId
                            or f.approver3UserId = :userId)))
            order by r.updatedAt desc
            """)
    List<RiskRecord> findVisibleToContributor(@Param("type") RiskType type,
                                              @Param("userId") Long userId);

    @Query("""
            select r.id from RiskRecord r
            where r.assignedContributorId = :userId
               or r.riskOwnerUserId = :userId
            """)
    List<Long> findIdsVisibleToContributor(@Param("userId") Long userId);

    List<RiskRecord> findByStatus(RiskStatus status);

    boolean existsByDisplayIdIgnoreCase(String displayId);

    /** {@code key} must be upper case with spaces, dashes and underscores removed. */
    @Query("""
            select r.displayId from RiskRecord r
            where replace(replace(replace(upper(r.displayId), ' ', ''), '-', ''), '_', '') = :key
            """)
    List<String> findDisplayIdsByKey(@Param("key") String key);

    boolean existsByRiskProjectIdAndTitleIgnoreCase(Long riskProjectId, String title);

    boolean existsByRiskProjectIdAndTitleIgnoreCaseAndIdNot(Long riskProjectId, String title, Long id);

    int countByRiskProjectId(Long riskProjectId);

    @Query("""
            select r.riskProjectId, count(r)
            from RiskRecord r
            where r.riskProjectId in :ids
            group by r.riskProjectId
            """)
    List<Object[]> countGroupedByRiskProjectId(@Param("ids") Collection<Long> ids);

    List<RiskRecord> findByRiskProjectIdOrderByUpdatedAtDesc(Long riskProjectId);

    List<RiskRecord> findByRiskTypeAndStatusOrderByUpdatedAtDesc(RiskType riskType, RiskStatus status);

    List<RiskRecord> findByRiskProjectIdAndStatusOrderByUpdatedAtDesc(Long riskProjectId, RiskStatus status);

    boolean existsBySourceDpiaAssessmentRowId(Long sourceDpiaAssessmentRowId);

    List<RiskRecord> findBySourceDpiaAssessmentRowIdIn(Collection<Long> sourceDpiaAssessmentRowIds);
}
