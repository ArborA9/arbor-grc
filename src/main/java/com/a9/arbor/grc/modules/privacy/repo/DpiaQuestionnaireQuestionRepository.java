package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DpiaQuestionnaireQuestionRepository extends JpaRepository<DpiaQuestionnaireQuestion, Long> {
    List<DpiaQuestionnaireQuestion> findByQuestionnaireIdOrderBySortOrderAscIdAsc(Long questionnaireId);

    Optional<DpiaQuestionnaireQuestion> findByIdAndQuestionnaireId(Long id, Long questionnaireId);

    boolean existsByQuestionnaireIdAndCodeIgnoreCase(Long questionnaireId, String code);

    boolean existsByQuestionnaireIdAndCodeIgnoreCaseAndIdNot(Long questionnaireId, String code, Long id);

    long countByQuestionnaireId(Long questionnaireId);

    void deleteByQuestionnaireId(Long questionnaireId);
}
