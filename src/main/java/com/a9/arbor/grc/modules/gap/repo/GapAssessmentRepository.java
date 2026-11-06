package com.a9.arbor.grc.modules.gap.repo;

import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GapAssessmentRepository extends JpaRepository<GapAssessment, Long> {
    List<GapAssessment> findAllByOrderByUpdatedAtDesc();

    List<GapAssessment> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    List<GapAssessment> findByFrameworkIdOrderByUpdatedAtDesc(Long frameworkId);

    List<GapAssessment> findByProjectIdAndFrameworkIdAndStatusIn(
            Long projectId, Long frameworkId, Collection<GapAssessmentStatus> statuses);

    long countByProjectId(Long projectId);
}
