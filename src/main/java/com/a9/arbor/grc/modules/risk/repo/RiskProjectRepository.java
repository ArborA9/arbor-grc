package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskProject;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskProjectRepository extends JpaRepository<RiskProject, Long> {
    List<RiskProject> findByProjectTypeOrderByYearDescUpdatedAtDesc(RiskType projectType);

    boolean existsByProjectTypeAndNameIgnoreCase(RiskType projectType, String name);
}
