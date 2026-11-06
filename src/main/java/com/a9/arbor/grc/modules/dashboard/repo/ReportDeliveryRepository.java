package com.a9.arbor.grc.modules.dashboard.repo;

import com.a9.arbor.grc.modules.dashboard.domain.ReportDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReportDeliveryRepository extends JpaRepository<ReportDelivery, Long> {
    List<ReportDelivery> findByRecipientUserIdOrderByDeliveredAtDesc(Long recipientUserId);

    void deleteByDeliveredAtBefore(Instant cutoff);
}
