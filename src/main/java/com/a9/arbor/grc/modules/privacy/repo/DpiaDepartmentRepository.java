package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DpiaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.DpiaDepartmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DpiaDepartmentRepository extends JpaRepository<DpiaDepartment, Long> {
    List<DpiaDepartment> findByDpiaProjectIdOrderByIdAsc(Long dpiaProjectId);

    boolean existsByDpiaProjectIdAndOrgUnitId(Long dpiaProjectId, Long orgUnitId);

    long countByDpiaProjectIdAndStatusNot(Long dpiaProjectId, DpiaDepartmentStatus status);

    long countByDpiaProjectId(Long dpiaProjectId);

    List<DpiaDepartment> findByQuestionnaireId(Long questionnaireId);
}
