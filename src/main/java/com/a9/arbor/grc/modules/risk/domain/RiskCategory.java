package com.a9.arbor.grc.modules.risk.domain;

public enum RiskCategory {
    STRATEGIC,
    OPERATIONAL,
    FINANCIAL,
    COMPLIANCE,
    IT,
    REPUTATIONAL,
    /** ERM field template (req §14). */
    LEGAL,
    ENVIRONMENTAL,
    /** Doc 03 D3-01: IS and ERM; requires categoryOther text. */
    OTHER
}
