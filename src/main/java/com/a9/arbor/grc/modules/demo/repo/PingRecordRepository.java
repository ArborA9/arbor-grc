package com.a9.arbor.grc.modules.demo.repo;

import com.a9.arbor.grc.modules.demo.domain.PingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PingRecordRepository extends JpaRepository<PingRecord, Long> {
}
