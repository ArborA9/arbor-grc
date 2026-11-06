package com.a9.arbor.grc.modules.risk.domain;

/** Which 5×5 plane the interactive heat map aggregates on (J8 / §19.2). */
public enum HeatMapScoreBasis {
    /** Probability (rows) × Impact (cols); band from inherent matrix. */
    INHERENT,
    /** Control effectiveness (rows) × Inherent risk (cols); band from residual matrix. */
    RESIDUAL
}
