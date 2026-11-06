package com.a9.arbor.grc.modules.dashboard.repo;

import com.a9.arbor.grc.modules.dashboard.domain.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {
    List<ReportSchedule> findByActiveTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant asOf);
}
