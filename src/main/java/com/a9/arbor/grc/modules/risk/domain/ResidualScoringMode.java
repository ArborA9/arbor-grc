package com.a9.arbor.grc.modules.risk.domain;

/**
 * How residual risk is scored after controls (client option):
 * auto from control effectiveness <strong>or</strong> manual residual Probability × Impact.
 */
public enum ResidualScoringMode {
    /** Residual = Control effectiveness × Inherent (§13.3 CE matrix). */
    CONTROL_EFFECTIVENESS,
    /** Residual = Probability × Impact after controls (Excel residual sheet). */
    MANUAL_PROBABILITY_IMPACT
}
