package com.a9.arbor.grc.modules.privacy.repo;

import com.a9.arbor.grc.modules.privacy.domain.DpiaProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DpiaProjectRepository extends JpaRepository<DpiaProject, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
