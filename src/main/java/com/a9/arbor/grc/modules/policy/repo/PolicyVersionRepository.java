package com.a9.arbor.grc.modules.policy.repo;

import com.a9.arbor.grc.modules.policy.domain.PolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, Long> {
    List<PolicyVersion> findByPolicyIdOrderByMajorVersionDescMinorVersionDesc(Long policyId);
    Optional<PolicyVersion> findByIdAndPolicyId(Long id, Long policyId);
}
