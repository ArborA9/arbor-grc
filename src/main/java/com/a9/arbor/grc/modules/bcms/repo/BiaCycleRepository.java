package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BiaCycle;
import com.a9.arbor.grc.modules.bcms.domain.BiaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiaCycleRepository extends JpaRepository<BiaCycle, Long> {
    List<BiaCycle> findAllByOrderByUpdatedAtDesc();
    List<BiaCycle> findByBiaProjectIdOrderByUpdatedAtDesc(Long biaProjectId);
    List<BiaCycle> findByOrgUnitIdOrderByUpdatedAtDesc(Long orgUnitId);
    List<BiaCycle> findByStatusOrderByUpdatedAtDesc(BiaStatus status);
    List<BiaCycle> findByAssignedToUserIdOrderByUpdatedAtDesc(Long userId);
    List<BiaCycle> findByBcHeadUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<BiaCycle> findByDisplayId(String displayId);
    boolean existsByDisplayId(String displayId);
    boolean existsByBiaProjectIdAndOrgUnitId(Long biaProjectId, Long orgUnitId);
}
