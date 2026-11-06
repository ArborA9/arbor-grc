package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BcmsExercisePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BcmsExercisePlanRepository extends JpaRepository<BcmsExercisePlan, BcmsExercisePlan.Pk> {
    List<BcmsExercisePlan> findByExerciseId(Long exerciseId);
    void deleteByExerciseId(Long exerciseId);
}
