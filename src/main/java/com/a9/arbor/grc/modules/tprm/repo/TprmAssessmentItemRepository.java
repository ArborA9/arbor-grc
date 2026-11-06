package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.TprmAssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TprmAssessmentItemRepository extends JpaRepository<TprmAssessmentItem, Long> {
    List<TprmAssessmentItem> findByAssessmentIdOrderBySortOrderAscIdAsc(Long assessmentId);

    Optional<TprmAssessmentItem> findByAssessmentIdAndControlId(Long assessmentId, Long controlId);

    Optional<TprmAssessmentItem> findByIdAndAssessmentId(Long id, Long assessmentId);
}
