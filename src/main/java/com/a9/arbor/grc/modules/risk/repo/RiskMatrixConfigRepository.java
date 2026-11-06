package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskMatrixConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskMatrixConfigRepository extends JpaRepository<RiskMatrixConfig, Long> {
    Optional<RiskMatrixConfig> findFirstByOrderByIdAsc();
}
