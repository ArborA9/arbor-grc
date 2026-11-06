package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DpiaInstanceQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DpiaInstanceQuestionRepository extends JpaRepository<DpiaInstanceQuestion, Long> {
    List<DpiaInstanceQuestion> findByDpiaDepartmentIdOrderBySortOrderAscIdAsc(Long dpiaDepartmentId);

    Optional<DpiaInstanceQuestion> findByIdAndDpiaDepartmentId(Long id, Long dpiaDepartmentId);

    void deleteByDpiaDepartmentId(Long dpiaDepartmentId);

    long countByDpiaDepartmentId(Long dpiaDepartmentId);
}
