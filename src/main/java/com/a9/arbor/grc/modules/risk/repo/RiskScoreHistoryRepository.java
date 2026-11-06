package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskScoreHistoryRepository extends JpaRepository<RiskScoreHistory, Long> {
    List<RiskScoreHistory> findByRiskIdOrderByRecordedAtAscIdAsc(long riskId);
}
