package com.a9.arbor.grc.modules.incident.repo;

import com.a9.arbor.grc.modules.incident.domain.IncidentRecord;
import com.a9.arbor.grc.modules.incident.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRecordRepository extends JpaRepository<IncidentRecord, Long> {

    List<IncidentRecord> findAllByOrderByUpdatedAtDesc();

    List<IncidentRecord> findByOwnerUserIdOrReporterUserIdOrderByUpdatedAtDesc(Long ownerUserId, Long reporterUserId);

    List<IncidentRecord> findByStatusInOrderByResponseDueAtAsc(List<IncidentStatus> statuses);
}
