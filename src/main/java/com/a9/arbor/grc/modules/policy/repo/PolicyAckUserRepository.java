package com.a9.arbor.grc.modules.policy.repo;

import com.a9.arbor.grc.modules.policy.domain.PolicyAckUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyAckUserRepository extends JpaRepository<PolicyAckUser, PolicyAckUser.Pk> {
    List<PolicyAckUser> findByPolicyId(Long policyId);
    void deleteByPolicyId(Long policyId);
}
