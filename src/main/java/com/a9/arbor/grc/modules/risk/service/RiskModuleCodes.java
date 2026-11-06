package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.risk.domain.RiskType;

/** Licence module for each risk register type. */
public final class RiskModuleCodes {

    private RiskModuleCodes() {
    }

    public static ModuleCode forType(RiskType type) {
        if (type == null) {
            return ModuleCode.ISMS;
        }
        return switch (type) {
            case ERM -> ModuleCode.ERM;
            case PRIVACY_RISK -> ModuleCode.PRIVACY;
            case IS_RISK -> ModuleCode.ISMS;
        };
    }
}
