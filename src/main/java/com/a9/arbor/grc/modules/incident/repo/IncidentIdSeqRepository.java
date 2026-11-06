package com.a9.arbor.grc.modules.incident.repo;

import com.a9.arbor.grc.modules.incident.domain.IncidentIdSeq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentIdSeqRepository extends JpaRepository<IncidentIdSeq, IncidentIdSeq.Pk> {
}
