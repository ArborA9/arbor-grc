package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.TreatmentMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentMilestoneRepository extends JpaRepository<TreatmentMilestone, Long> {
    List<TreatmentMilestone> findByTreatmentPlanIdOrderByDueDateAscIdAsc(Long treatmentPlanId);
}
