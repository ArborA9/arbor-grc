package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DpiaDpoAssessmentRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DpiaDpoAssessmentRowRepository extends JpaRepository<DpiaDpoAssessmentRow, Long> {
    List<DpiaDpoAssessmentRow> findByDpiaDepartmentIdOrderBySortOrderAscIdAsc(Long dpiaDepartmentId);

    Optional<DpiaDpoAssessmentRow> findByIdAndDpiaDepartmentId(Long id, Long dpiaDepartmentId);

    void deleteByDpiaDepartmentId(Long dpiaDepartmentId);

    long countByDpiaDepartmentId(Long dpiaDepartmentId);
}
