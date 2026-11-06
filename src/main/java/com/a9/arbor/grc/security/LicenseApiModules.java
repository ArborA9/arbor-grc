package com.a9.arbor.grc.security;

import com.a9.arbor.grc.catalog.domain.ModuleCode;

/**
 * Maps tenant API paths that belong to exactly one commercial module.
 * Mixed paths (risk, Gap, SoA, CAPA, frameworks, evidence) return null so the
 * interceptor / service can lock the specific record's module.
 */
public final class LicenseApiModules {

    private LicenseApiModules() {
    }

    public static ModuleCode exclusiveModule(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (starts(path, "/api/assets")) {
            return ModuleCode.ISMS;
        }
        if (starts(path, "/api/privacy")) {
            return ModuleCode.PRIVACY;
        }
        if (starts(path, "/api/tprm") || starts(path, "/api/kri") || starts(path, "/api/incidents")) {
            return ModuleCode.ERM;
        }
        if (starts(path, "/api/bcms")) {
            return ModuleCode.BCMS;
        }
        if (starts(path, "/api/audits")
                && !starts(path, "/api/audits/external-access")
                && !starts(path, "/api/audit-trail")) {
            return ModuleCode.AUDIT;
        }
        return null;
    }

    private static boolean starts(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
