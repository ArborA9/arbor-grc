package com.a9.arbor.grc.modules.evidence.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Canonical workflow stages for evidence linked to RISK (§13.7 / §22 / J3).
 * Other modules (Gap, CAPA) keep free-form stage strings.
 */
public enum RiskEvidenceStage {
    IDENTIFICATION,
    ASSIGNMENT,
    REVIEW,
    TREATMENT,
    TOLERANCE,
    TRANSFER;

    private static final Set<String> NAMES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * When linking to RISK, stage is required and must be one of the enum values
     * (case-insensitive). Returns normalized UPPER_SNAKE name.
     */
    public static String requireForRiskLink(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "workflowStage is required when linking evidence to RISK "
                            + "(IDENTIFICATION|ASSIGNMENT|REVIEW|TREATMENT|TOLERANCE|TRANSFER)");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (!NAMES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid RISK workflowStage: " + raw
                            + ": use IDENTIFICATION|ASSIGNMENT|REVIEW|TREATMENT|TOLERANCE|TRANSFER");
        }
        return normalized;
    }

    public static boolean isRiskModule(String moduleCode) {
        return moduleCode != null && "RISK".equalsIgnoreCase(moduleCode.trim());
    }
}
