package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;

import java.util.EnumSet;
import java.util.Set;

/**
 * Frameworks that may appear on an Information Security Risk project.
 * Arbor: ISMS (all), ERM, ITSM, AIMS, BCMS. Not Privacy / QMS / OHSMS / SAMA.
 */
public final class IsRiskApplicableModules {

    public static final Set<ModuleCode> MODULES = EnumSet.of(
            ModuleCode.ISMS,
            ModuleCode.ERM,
            ModuleCode.ITSM,
            ModuleCode.AIMS,
            ModuleCode.BCMS);

    private IsRiskApplicableModules() {
    }

    public static boolean includes(ModuleCode code) {
        return code != null && MODULES.contains(code);
    }
}
