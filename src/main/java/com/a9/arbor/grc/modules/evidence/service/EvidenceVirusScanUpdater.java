package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceVersion;
import com.a9.arbor.grc.modules.evidence.domain.VirusScanStatus;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-DB write. Caller must set {@link com.a9.arbor.grc.tenancy.TenantContext} first.
 */
@Service
public class EvidenceVirusScanUpdater {

    private final EvidenceVersionRepository versionRepository;

    public EvidenceVirusScanUpdater(EvidenceVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Transactional
    public boolean apply(String storageKey, VirusScanStatus status) {
        EvidenceVersion version = versionRepository.findByStorageKey(storageKey).orElse(null);
        if (version == null) {
            return false;
        }
        if (version.getVirusScanStatus() == status) {
            return true;
        }
        version.setVirusScanStatus(status);
        versionRepository.save(version);
        return true;
    }
}
