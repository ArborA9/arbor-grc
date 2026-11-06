package com.a9.arbor.grc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Evidence file storage + scan policy.
 * {@code storage=local} (default) or {@code blob} (Azure UAE North when connection string set).
 */
@Data
@ConfigurationProperties(prefix = "arbor.evidence")
public class ArborEvidenceProperties {

    /**
     * {@code local}: disk under {@link #storageRoot}; {@code blob}: Azure Blob
     * ({@link #blobConnectionString} required).
     */
    private String storage = "local";

    /** Root directory for tenant folders when storage=local. */
    private String storageRoot = ".run/evidence";

    /** Azure Blob connection string (Key Vault / env in prod). */
    private String blobConnectionString = "";

    /** Blob container name (created if missing). */
    private String blobContainer = "arbor-evidence";

    /** Max upload size in bytes (req: 25MB). */
    private long maxFileBytes = 25L * 1024 * 1024;

    /** Allowed extensions without leading dot. */
    private String allowedExtensions = "pdf,docx,xlsx,png,jpg,jpeg";

    /**
     * Status written on upload when no scanner is wired.
     * Never use CLEAN here: only a real scanner may set CLEAN/INFECTED.
     * {@code SKIPPED} = local/dev honesty; {@code PENDING} = awaiting scanner (Blob/prod).
     */
    private String virusScanOnUpload = "SKIPPED";

    /** Days before expiry to fire EVIDENCE_EXPIRING (req §7 trigger 87). */
    private int expiryWarnDays = 30;

    /**
     * When true, evidence download is blocked unless the version virus scan status is {@code CLEAN}.
     * Default false for local dev; enable in azure profile until a real scanner is wired.
     */
    private boolean blockDownloadUnlessClean = false;

    /**
     * Shared secret for {@code POST /api/webhooks/malware-scan?token=}. Empty disables scan updates.
     * The Event Grid subscription handshake still works.
     */
    private String malwareWebhookToken = "";
}
