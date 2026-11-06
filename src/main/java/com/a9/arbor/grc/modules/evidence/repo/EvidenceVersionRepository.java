package com.a9.arbor.grc.modules.evidence.repo;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvidenceVersionRepository extends JpaRepository<EvidenceVersion, Long> {

    List<EvidenceVersion> findByEvidenceIdOrderByVersionNumberDesc(Long evidenceId);

    Optional<EvidenceVersion> findByEvidenceIdAndVersionNumber(Long evidenceId, int versionNumber);

    Optional<EvidenceVersion> findByStorageKey(String storageKey);
}
