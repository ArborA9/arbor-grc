package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoveryPlanRepository extends JpaRepository<RecoveryPlan, Long> {
    List<RecoveryPlan> findAllByOrderByUpdatedAtDesc();
    List<RecoveryPlan> findByProjectIdOrderByUpdatedAtDesc(Long projectId);
}
