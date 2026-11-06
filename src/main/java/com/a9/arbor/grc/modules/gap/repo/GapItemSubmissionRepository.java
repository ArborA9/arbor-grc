package com.a9.arbor.grc.modules.gap.repo;

import com.a9.arbor.grc.modules.gap.domain.GapItemSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GapItemSubmissionRepository extends JpaRepository<GapItemSubmission, Long> {

    List<GapItemSubmission> findByGapItemIdOrderBySubmissionNumberAsc(Long gapItemId);

    @Query("select coalesce(max(s.submissionNumber), 0) from GapItemSubmission s where s.gapItemId = :gapItemId")
    int maxSubmissionNumber(Long gapItemId);

    Optional<GapItemSubmission> findTopByGapItemIdOrderBySubmissionNumberDesc(Long gapItemId);
}
