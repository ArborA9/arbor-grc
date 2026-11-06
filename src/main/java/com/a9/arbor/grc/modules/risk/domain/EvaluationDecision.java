package com.a9.arbor.grc.modules.risk.domain;

/** Excel evaluation: residual ≤ auto-close / within appetite / treatment required. */
public enum EvaluationDecision {
    AUTO_CLOSE,
    MONITOR,
    TREATMENT_REQUIRED
}
