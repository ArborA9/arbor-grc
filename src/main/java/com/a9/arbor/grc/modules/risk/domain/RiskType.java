package com.a9.arbor.grc.modules.risk.domain;

public enum RiskType {
    IS_RISK,
    ERM,
    PRIVACY_RISK;

    /**
     * ERM and Unified Risk (IS_RISK): FCA Likelihood / rating labels and manual residual as L×I
     * dropdowns with axis caps. Privacy Risk keeps typed residual score and Excel VL…VH words.
     */
    public boolean usesFcaMatrixLabels() {
        return this == ERM || this == IS_RISK;
    }
}
