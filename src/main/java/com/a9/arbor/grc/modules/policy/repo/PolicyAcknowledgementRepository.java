package com.a9.arbor.grc.modules.policy.repo;

import com.a9.arbor.grc.modules.policy.domain.PolicyAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyAcknowledgementRepository extends JpaRepository<PolicyAcknowledgement, Long> {
    List<PolicyAcknowledgement> findByPolicyVersionIdOrderByAcknowledgedAtAsc(Long policyVersionId);
    Optional<PolicyAcknowledgement> findByPolicyVersionIdAndUserId(Long policyVersionId, Long userId);
    long countByPolicyVersionId(Long policyVersionId);
    boolean existsByPolicyVersionIdAndUserId(Long policyVersionId, Long userId);
}
