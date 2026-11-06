package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.RopaDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RopaDepartmentRepository extends JpaRepository<RopaDepartment, Long> {
    List<RopaDepartment> findByRopaProjectIdOrderByCreatedAtAsc(Long projectId);
    List<RopaDepartment> findByChampionUserIdOrderByUpdatedAtDesc(Long championUserId);
    List<RopaDepartment> findByDepartmentHeadUserIdOrderByUpdatedAtDesc(Long headUserId);
    boolean existsByRopaProjectIdAndOrgUnitId(Long projectId, Long orgUnitId);
}
