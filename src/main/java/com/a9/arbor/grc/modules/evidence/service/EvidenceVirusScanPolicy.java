package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.config.ArborEvidenceProperties;
import com.a9.arbor.grc.modules.evidence.domain.VirusScanStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Maps configured upload scan mode → stored status. Never invents CLEAN without a scanner.
 */
@Component
public class EvidenceVirusScanPolicy {

    private final VirusScanStatus onUpload;

    public EvidenceVirusScanPolicy(ArborEvidenceProperties properties) {
        this.onUpload = parseOnUpload(properties.getVirusScanOnUpload());
    }

    public VirusScanStatus statusForNewUpload() {
        return onUpload;
    }

    /** Human-readable label: never implies clean when skipped/pending. */
    public static String displayLabel(VirusScanStatus status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case CLEAN -> "Clean";
            case INFECTED -> "Infected: blocked";
            case PENDING -> "Scan pending: not verified clean";
            case SKIPPED -> "Not scanned: not verified clean";
        };
    }

    private static VirusScanStatus parseOnUpload(String raw) {
        if (raw == null || raw.isBlank()) {
            return VirusScanStatus.SKIPPED;
        }
        try {
            VirusScanStatus s = VirusScanStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            if (s == VirusScanStatus.CLEAN || s == VirusScanStatus.INFECTED) {
                throw new IllegalStateException(
                        "arbor.evidence.virus-scan-on-upload cannot be CLEAN or INFECTED "
                                + "(only a real scanner may set those). Use SKIPPED or PENDING.");
            }
            return s;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid arbor.evidence.virus-scan-on-upload: " + raw, ex);
        }
    }
}
