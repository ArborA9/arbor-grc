package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.modules.evidence.domain.RiskEvidenceStage;

import java.util.Locale;

/**
 * Evidence linked to an IS Risk / ERM record, a Gap control, or a Gap question:
 * 5 files per user per record, 10MB each. CAPA / TPRM keep the global evidence cap.
 */
public final class RiskEvidenceLimits {

    public static final int MAX_FILES_PER_USER = 5;
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private static final String GAP_MODULE = "GAP";

    private RiskEvidenceLimits() {
    }

    public static boolean appliesTo(String moduleCode) {
        return RiskEvidenceStage.isRiskModule(moduleCode)
                || (moduleCode != null && GAP_MODULE.equals(moduleCode.trim().toUpperCase(Locale.ROOT)));
    }

    /** Message noun for the file-count error, e.g. "this risk". */
    public static String recordNoun(String moduleCode, String recordType) {
        if (moduleCode != null && GAP_MODULE.equals(moduleCode.trim().toUpperCase(Locale.ROOT))) {
            return "GAP_QUESTION".equals(recordType == null ? null : recordType.trim())
                    ? "this question"
                    : "this control";
        }
        return "this risk";
    }
}
