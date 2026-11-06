package com.a9.arbor.grc.modules.project.repo;

import com.a9.arbor.grc.modules.project.domain.ComplianceProjectFramework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplianceProjectFrameworkRepository extends JpaRepository<ComplianceProjectFramework, Long> {
    List<ComplianceProjectFramework> findByProjectIdOrderByFrameworkCodeAsc(Long projectId);

    Optional<ComplianceProjectFramework> findByProjectIdAndFrameworkId(Long projectId, Long frameworkId);

    boolean existsByProjectIdAndFrameworkId(Long projectId, Long frameworkId);
}
