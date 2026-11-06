package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.AssessmentStatus;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TprmAssessmentRepository extends JpaRepository<TprmAssessment, Long> {
    List<TprmAssessment> findByVendorIdOrderByCreatedAtDesc(Long vendorId);

    List<TprmAssessment> findAllByOrderByCreatedAtDesc();

    long countByDisplayIdStartingWith(String prefix);

    List<TprmAssessment> findByVendorIdAndStatusIn(Long vendorId, List<AssessmentStatus> statuses);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);
}
