package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskControlRepository extends JpaRepository<RiskControl, Long> {

    List<RiskControl> findByRiskIdOrderBySortOrderAscIdAsc(Long riskId);

    void deleteByRiskId(Long riskId);
}
