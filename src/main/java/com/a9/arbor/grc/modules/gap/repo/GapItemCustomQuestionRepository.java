package com.a9.arbor.grc.modules.gap.repo;

import com.a9.arbor.grc.modules.gap.domain.GapItemCustomQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GapItemCustomQuestionRepository extends JpaRepository<GapItemCustomQuestion, Long> {
    List<GapItemCustomQuestion> findByGapItemIdOrderBySortOrderAscIdAsc(Long gapItemId);
}
