package com.a9.arbor.grc.modules.risk.repo;

import com.a9.arbor.grc.modules.risk.domain.AcceptanceFormStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskAcceptanceForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskAcceptanceFormRepository extends JpaRepository<RiskAcceptanceForm, Long> {
    Optional<RiskAcceptanceForm> findByRiskId(Long riskId);

    List<RiskAcceptanceForm> findByRiskIdIn(java.util.Collection<Long> riskIds);

    List<RiskAcceptanceForm> findByStatusOrderByUpdatedAtDesc(AcceptanceFormStatus status);

    List<RiskAcceptanceForm> findByStatusInOrderByUpdatedAtDesc(java.util.Collection<AcceptanceFormStatus> statuses);

    List<RiskAcceptanceForm> findByStatusAndApprover1UserId(AcceptanceFormStatus status, Long approver1UserId);

    List<RiskAcceptanceForm> findByStatusAndApprover2UserId(AcceptanceFormStatus status, Long approver2UserId);

    List<RiskAcceptanceForm> findByStatusAndApprover3UserId(AcceptanceFormStatus status, Long approver3UserId);

    List<RiskAcceptanceForm> findByApprover1UserIdAndStatusIn(Long approver1UserId,
                                                             java.util.Collection<AcceptanceFormStatus> statuses);

    List<RiskAcceptanceForm> findByApprover2UserIdAndStatusIn(Long approver2UserId,
                                                             java.util.Collection<AcceptanceFormStatus> statuses);

    List<RiskAcceptanceForm> findByApprover3UserIdAndStatusIn(Long approver3UserId,
                                                             java.util.Collection<AcceptanceFormStatus> statuses);
}
