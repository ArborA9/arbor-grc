package com.a9.arbor.grc.modules.tprm.repo;

import com.a9.arbor.grc.modules.tprm.domain.TprmQuestionnaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TprmQuestionnaireRepository extends JpaRepository<TprmQuestionnaire, Long> {
    List<TprmQuestionnaire> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
