package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskIdSeq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskIdSeqRepository extends JpaRepository<RiskIdSeq, RiskIdSeq.Pk> {
}
