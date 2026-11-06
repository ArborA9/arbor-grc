package com.a9.arbor.grc.modules.evidence.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Evidence binary storage. Local FS for dev; Azure Blob (UAE North) when
 * {@code arbor.evidence.storage=blob}.
 */
public interface EvidenceFileStore {

    /** Backend id for diagnostics ("local" or "blob"). */
    String backend();

    StoredFile store(long evidenceId, int versionNumber, MultipartFile file);

    StoredFile storeBytes(long evidenceId, int versionNumber,
                          String originalFilename, String contentType, byte[] bytes);

    Resource open(String storageKey);

    record StoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes) {
    }
}
