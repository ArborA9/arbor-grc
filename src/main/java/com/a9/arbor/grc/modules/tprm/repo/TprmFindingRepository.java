package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.FindingStatus;
import com.a9.arbor.grc.modules.tprm.domain.TprmFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TprmFindingRepository extends JpaRepository<TprmFinding, Long> {
    List<TprmFinding> findByVendorIdOrderByCreatedAtDesc(Long vendorId);

    List<TprmFinding> findByAssessmentIdOrderByIdAsc(Long assessmentId);

    List<TprmFinding> findAllByOrderByCreatedAtDesc();

    List<TprmFinding> findByStatus(FindingStatus status);
}
