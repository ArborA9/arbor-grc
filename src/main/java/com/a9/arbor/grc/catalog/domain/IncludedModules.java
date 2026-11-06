package com.a9.arbor.grc.catalog.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * AUDIT (with CAPA) is not sold on its own: it comes with any paid module (Arbor UAT 14 Oct 2026).
 * Its stored row stays enabled with no term of its own; the effective licence is derived from the
 * paid rows: on while any paid module is licensed, expired only once every paid module has expired.
 */
public final class IncludedModules {

    private IncludedModules() {
    }

    public static boolean isIncluded(ModuleCode code) {
        return code == ModuleCode.AUDIT;
    }

    /** Copy of {@code rows} with each included module's row replaced by its derived licence. */
    public static List<TenantModule> effective(Collection<TenantModule> rows) {
        List<TenantModule> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (TenantModule row : rows) {
            if (row != null && isIncluded(row.getModuleCode())) {
                out.add(derived(row, rows));
            } else {
                out.add(row);
            }
        }
        return out;
    }

    /** Derived licence for an included module. Transient: never save the returned row. */
    public static TenantModule derived(TenantModule stored, Collection<TenantModule> rows) {
        boolean anyPaid = false;
        boolean openEnded = false;
        Instant latestExpiry = null;
        Instant firstEnabled = null;
        for (TenantModule row : rows) {
            if (row == null || !isPaid(row.getModuleCode()) || !row.isEnabled()) {
                continue;
            }
            anyPaid = true;
            if (row.getExpiresAt() == null) {
                openEnded = true;
            } else if (latestExpiry == null || row.getExpiresAt().isAfter(latestExpiry)) {
                latestExpiry = row.getExpiresAt();
            }
            if (row.getEnabledAt() != null && (firstEnabled == null || row.getEnabledAt().isBefore(firstEnabled))) {
                firstEnabled = row.getEnabledAt();
            }
        }
        TenantModule out = new TenantModule();
        out.setTenantId(stored.getTenantId());
        out.setModuleCode(stored.getModuleCode());
        out.setEnabled(anyPaid);
        out.setEnabledAt(anyPaid ? firstEnabled : null);
        out.setExpiresAt(anyPaid && !openEnded ? latestExpiry : null);
        return out;
    }

    private static boolean isPaid(ModuleCode code) {
        return code != null && code.delivered() && !isIncluded(code);
    }
}
