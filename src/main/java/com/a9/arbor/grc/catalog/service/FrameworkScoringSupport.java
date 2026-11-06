package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;

import java.util.Locale;

/** Detects score-based (UAE IA / NCEMA) vs standard compliance frameworks. */
public final class FrameworkScoringSupport {

    private FrameworkScoringSupport() {
    }

    /**
     * A bare {@code "UAE"} token used to be enough here, which also matched <b>UAE PDPL</b>: that
     * would have run PDPL gap assessments through {@link
     * com.a9.arbor.grc.modules.gap.service.UaeIaGapScoring}, whose layers key off an {@code M}/{@code T}
     * code prefix that {@code Art.4} does not have, and offered PDPL an ASSESSMENT-mode audit. The
     * fallback is narrowed rather than deleted because persisted {@code gap_assessment} rows keep a
     * copy of the framework code and name from when they were created.
     */
    public static boolean isUaeIa(String code, String name) {
        if (matchesType(code, FrameworkTypeCode.UAE_IA)) {
            return true;
        }
        return containsToken(code, "UAE IA") || containsToken(name, "UAE IA");
    }

    public static boolean isNcema(String code, String name) {
        if (matchesType(code, FrameworkTypeCode.NCEMA_V3)) {
            return true;
        }
        return containsToken(code, "NCEMA") || containsToken(name, "NCEMA");
    }

    public static boolean isSama(String code, String name) {
        if (matchesType(code, FrameworkTypeCode.SAMA_CSF)) {
            return true;
        }
        return containsToken(code, "SAMA") || containsToken(name, "SAMA");
    }

    /** UAE IA and NCEMA use assessment (score-based) audits, not finding severity. */
    public static boolean isAssessmentFramework(String code, String name) {
        return isUaeIa(code, name) || isNcema(code, name);
    }

    private static boolean matchesType(String code, FrameworkTypeCode type) {
        if (code == null || code.isBlank()) {
            return false;
        }
        try {
            return FrameworkTypeCode.valueOf(code.trim().toUpperCase(Locale.ROOT)) == type;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Underscores fold to spaces so a {@code UAE_IA} code and a {@code "UAE IA (TDRA)"} name match alike. */
    private static boolean containsToken(String value, String token) {
        return value != null
                && value.toUpperCase(Locale.ROOT).replace('_', ' ').contains(token);
    }
}
