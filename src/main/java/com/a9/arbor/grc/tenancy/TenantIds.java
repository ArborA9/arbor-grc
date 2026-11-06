package com.a9.arbor.grc.tenancy;

import java.util.Locale;

/**
 * Canonical tenant-id rules: operators may type mixed case; storage and lookups use lowercase.
 */
public final class TenantIds {

    private TenantIds() {
    }

    /** Trim and lower-case; blank stays blank. */
    public static String normalize(String tenantId) {
        if (tenantId == null) {
            return null;
        }
        String trimmed = tenantId.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
