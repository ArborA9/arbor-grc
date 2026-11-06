package com.a9.arbor.grc.modules.evidence.domain;

/** Virus scan result. Upload policy sets SKIPPED (local) or PENDING (awaiting scanner): never CLEAN without a real scan. */
public enum VirusScanStatus {
    PENDING,
    CLEAN,
    INFECTED,
    SKIPPED
}
