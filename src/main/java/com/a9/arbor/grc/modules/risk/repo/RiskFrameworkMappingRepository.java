package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskFrameworkMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskFrameworkMappingRepository extends JpaRepository<RiskFrameworkMapping, Long> {
    List<RiskFrameworkMapping> findByRiskIdOrderByIdAsc(Long riskId);

    void deleteByRiskId(Long riskId);
}
