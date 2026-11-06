package com.a9.arbor.grc.catalog.repo;

import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FrameworkNodeRepository extends JpaRepository<FrameworkNode, Long> {

    List<FrameworkNode> findByFrameworkIdOrderByLevelAscDisplayOrderAscNodeCodeAsc(Long frameworkId);

    /** Excel display order primary (Framework Import V2 Phase 3). */
    List<FrameworkNode> findByFrameworkIdOrderByDisplayOrderAscNodeCodeAsc(Long frameworkId);

    Optional<FrameworkNode> findByFrameworkIdAndNodeCode(Long frameworkId, String nodeCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FrameworkNode n set n.parentId = null where n.frameworkId = :frameworkId")
    void clearParents(@Param("frameworkId") Long frameworkId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FrameworkNode n where n.frameworkId = :frameworkId")
    void deleteByFrameworkId(@Param("frameworkId") Long frameworkId);

    long countByFrameworkIdAndLevelGreaterThanEqual(Long frameworkId, int level);

    long countByFrameworkIdAndRecordType(Long frameworkId, String recordType);

    @Query("""
            select n.frameworkId,
                   sum(case when n.inSoa = true then 1 else 0 end),
                   sum(case when n.inGapAssessment = true then 1 else 0 end),
                   sum(case when n.inAuditChecklist = true then 1 else 0 end),
                   sum(case when n.inRiskRegister = true then 1 else 0 end)
            from FrameworkNode n
            where n.frameworkId in :frameworkIds
            group by n.frameworkId
            """)
    List<Object[]> routingCounts(@Param("frameworkIds") java.util.Collection<Long> frameworkIds);
}
