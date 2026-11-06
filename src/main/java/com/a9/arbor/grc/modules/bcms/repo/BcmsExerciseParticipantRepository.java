package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BcmsExerciseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BcmsExerciseParticipantRepository extends JpaRepository<BcmsExerciseParticipant, Long> {
    List<BcmsExerciseParticipant> findByExerciseId(Long exerciseId);
    void deleteByExerciseId(Long exerciseId);
}
