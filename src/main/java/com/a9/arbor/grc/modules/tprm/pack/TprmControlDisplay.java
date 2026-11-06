package com.a9.arbor.grc.modules.tprm.pack;

import com.a9.arbor.grc.modules.tprm.domain.TprmAssessmentItem;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;

/** Prefer questionnaire/assessment snapshot text over the shared library row. */
public final class TprmControlDisplay {

    private TprmControlDisplay() {
    }

    public static String first(String snapshot, String fallback) {
        if (snapshot != null && !snapshot.isBlank()) {
            return snapshot.trim();
        }
        return fallback != null ? fallback : "";
    }

    public static String code(TprmAssessmentItem item, TprmControl control) {
        return first(item.getControlCode(), control != null ? control.getControlCode() : null);
    }

    public static String domain(TprmAssessmentItem item, TprmControl control) {
        return first(item.getDomain(), control != null ? control.getDomain() : null);
    }

    public static String requirement(TprmAssessmentItem item, TprmControl control) {
        return first(item.getRequirement(), control != null ? control.getRequirement() : null);
    }

    public static String requiredEvidence(TprmAssessmentItem item, TprmControl control) {
        return first(item.getRequiredEvidence(), control != null ? control.getRequiredEvidence() : null);
    }
}
