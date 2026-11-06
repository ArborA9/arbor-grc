package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanProcedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoveryPlanProcedureRepository extends JpaRepository<RecoveryPlanProcedure, Long> {
    List<RecoveryPlanProcedure> findByPlanIdOrderBySortOrderAscIdAsc(Long planId);
    void deleteByPlanId(Long planId);
}
