package com.a9.arbor.grc.modules.policy.repo;

import com.a9.arbor.grc.modules.policy.domain.PolicyFramework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyFrameworkRepository extends JpaRepository<PolicyFramework, PolicyFramework.Pk> {
    List<PolicyFramework> findByPolicyId(Long policyId);
    void deleteByPolicyId(Long policyId);
}
