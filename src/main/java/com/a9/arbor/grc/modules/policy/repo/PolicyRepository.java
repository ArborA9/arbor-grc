package com.a9.arbor.grc.modules.policy.repo;

import com.a9.arbor.grc.modules.policy.domain.Policy;
import com.a9.arbor.grc.modules.policy.domain.PolicyCategory;
import com.a9.arbor.grc.modules.policy.domain.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findAllByOrderByUpdatedAtDesc();
    List<Policy> findByCategoryOrderByUpdatedAtDesc(PolicyCategory category);
    List<Policy> findByStatusOrderByUpdatedAtDesc(PolicyStatus status);
    List<Policy> findByStatusAndReviewDateLessThanEqual(PolicyStatus status, LocalDate date);
    List<Policy> findByStatusAndReviewDateBetween(PolicyStatus status, LocalDate from, LocalDate to);
    List<Policy> findByStatusAndAckRequiredTrueAndAckDeadlineNotNull(PolicyStatus status);
}
