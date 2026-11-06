package com.a9.arbor.grc.modules.bcms.repo;

import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.domain.BiaReviewOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiaProcessRepository extends JpaRepository<BiaProcess, Long> {
    List<BiaProcess> findByCycleIdOrderBySortOrderAscIdAsc(Long cycleId);
    Optional<BiaProcess> findByCycleIdAndProcessCode(Long cycleId, String processCode);
    boolean existsByCycleIdAndProcessCode(Long cycleId, String processCode);
    int countByCycleId(Long cycleId);
    List<BiaProcess> findByReviewOutcome(BiaReviewOutcome outcome);
    List<BiaProcess> findAllByOrderByUpdatedAtDesc();
}
