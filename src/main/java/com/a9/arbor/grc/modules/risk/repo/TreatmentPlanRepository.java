package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {
    Optional<TreatmentPlan> findByRiskId(Long riskId);

    @Query("""
            select p from TreatmentPlan p
            where p.targetDate is not null
              and p.targetDate < :asOf
              and p.status <> com.a9.arbor.grc.modules.risk.domain.TreatmentPlanStatus.COMPLETED
            """)
    List<TreatmentPlan> findPastTargetNotCompleted(@Param("asOf") LocalDate asOf);
}
