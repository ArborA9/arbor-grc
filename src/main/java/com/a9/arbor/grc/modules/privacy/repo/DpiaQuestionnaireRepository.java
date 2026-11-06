package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DpiaQuestionnaireRepository extends JpaRepository<DpiaQuestionnaire, Long> {
    List<DpiaQuestionnaire> findAllByOrderBySystemDefaultDescNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<DpiaQuestionnaire> findFirstBySystemDefaultTrue();

    boolean existsBySystemDefaultTrue();

    List<DpiaQuestionnaire> findBySystemDefaultTrueOrderByIdAsc();
}
