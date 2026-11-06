package com.a9.arbor.grc.modules.policy.repo;

import com.a9.arbor.grc.modules.policy.domain.PolicyAckOrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyAckOrgUnitRepository extends JpaRepository<PolicyAckOrgUnit, PolicyAckOrgUnit.Pk> {
    List<PolicyAckOrgUnit> findByPolicyId(Long policyId);
    void deleteByPolicyId(Long policyId);
}
