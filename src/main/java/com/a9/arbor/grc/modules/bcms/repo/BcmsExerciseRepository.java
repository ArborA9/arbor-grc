package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BcmsExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BcmsExerciseRepository extends JpaRepository<BcmsExercise, Long> {
    List<BcmsExercise> findAllByOrderByUpdatedAtDesc();
    List<BcmsExercise> findByProjectIdOrderByUpdatedAtDesc(Long projectId);
}
