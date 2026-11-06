package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Default engagement read-scope modules from the licensed framework module. */
public final class EngagementScopeDefaults {

    private static final Set<EngagementScopeModule> CORE = EnumSet.of(
            EngagementScopeModule.GAP,
            EngagementScopeModule.SOA,
            EngagementScopeModule.RISK);

    private EngagementScopeDefaults() {
    }

    public static List<EngagementScopeModule> forFrameworkModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return new ArrayList<>(CORE);
        }
        return switch (ModuleCode.valueOf(moduleCode.trim().toUpperCase())) {
            case ISMS -> List.of(
                    EngagementScopeModule.GAP,
                    EngagementScopeModule.SOA,
                    EngagementScopeModule.RISK);
            case ERM -> List.of(EngagementScopeModule.RISK);
            case BCMS -> List.of(EngagementScopeModule.GAP);
            case PRIVACY -> List.of(EngagementScopeModule.GAP);
            default -> new ArrayList<>(CORE);
        };
    }
}
