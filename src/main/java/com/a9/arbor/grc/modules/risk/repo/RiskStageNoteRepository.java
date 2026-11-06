package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskStageNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskStageNoteRepository extends JpaRepository<RiskStageNote, Long> {
    List<RiskStageNote> findByRiskIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(long riskId);

    Optional<RiskStageNote> findByIdAndRiskIdAndDeletedAtIsNull(long id, long riskId);
}
