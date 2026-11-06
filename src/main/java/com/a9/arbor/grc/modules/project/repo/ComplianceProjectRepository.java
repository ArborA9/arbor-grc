package com.a9.arbor.grc.modules.project.repo;

import com.a9.arbor.grc.modules.project.domain.ComplianceProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceProjectRepository extends JpaRepository<ComplianceProject, Long> {
    List<ComplianceProject> findAllByOrderByYearDescUpdatedAtDesc();

    boolean existsByNameIgnoreCase(String name);
}
