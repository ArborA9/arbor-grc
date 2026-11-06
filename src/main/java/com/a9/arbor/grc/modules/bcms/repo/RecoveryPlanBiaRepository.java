package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanBia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoveryPlanBiaRepository extends JpaRepository<RecoveryPlanBia, RecoveryPlanBia.Pk> {
    List<RecoveryPlanBia> findByPlanId(Long planId);
    void deleteByPlanId(Long planId);
}
