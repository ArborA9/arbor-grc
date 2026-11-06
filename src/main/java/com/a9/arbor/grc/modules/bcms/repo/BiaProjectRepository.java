package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BiaProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BiaProjectRepository extends JpaRepository<BiaProject, Long> {
    List<BiaProject> findAllByOrderByYearDesc();
    boolean existsByNameIgnoreCase(String name);
}
