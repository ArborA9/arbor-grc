package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.RiskFormDraft;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskFormDraftRepository extends JpaRepository<RiskFormDraft, Long> {

    List<RiskFormDraft> findByCreatedByUserIdAndRiskTypeAndRiskProjectIdOrderByUpdatedAtDesc(
            Long createdByUserId, RiskType riskType, Long riskProjectId);

    Optional<RiskFormDraft> findByIdAndCreatedByUserId(Long id, Long createdByUserId);
}
