package com.a9.arbor.grc.modules.gap.repo;

import com.a9.arbor.grc.modules.gap.domain.GapItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GapItemRepository extends JpaRepository<GapItem, Long> {
    List<GapItem> findByGapAssessmentIdOrderByNodeCodeAsc(Long gapAssessmentId);

    Optional<GapItem> findByIdAndGapAssessmentId(Long id, Long gapAssessmentId);

    Optional<GapItem> findByGapAssessmentIdAndFrameworkNodeId(Long gapAssessmentId, Long frameworkNodeId);

    long countByGapAssessmentIdAndApplicabilityOverrideTrue(Long gapAssessmentId);

    List<GapItem> findByAssignedToUserId(Long assignedToUserId);
}
