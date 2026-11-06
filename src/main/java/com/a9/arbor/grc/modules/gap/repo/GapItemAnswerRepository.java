package com.a9.arbor.grc.modules.gap.repo;

import com.a9.arbor.grc.modules.gap.domain.GapItemAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GapItemAnswerRepository extends JpaRepository<GapItemAnswer, Long> {
    List<GapItemAnswer> findByGapItemId(Long gapItemId);
    Optional<GapItemAnswer> findByGapItemIdAndFrameworkQuestionId(Long gapItemId, Long frameworkQuestionId);
    Optional<GapItemAnswer> findByGapItemIdAndCustomQuestionId(Long gapItemId, Long customQuestionId);
    void deleteByGapItemId(Long gapItemId);
}
