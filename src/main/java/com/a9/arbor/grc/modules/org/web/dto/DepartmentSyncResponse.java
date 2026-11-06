package com.a9.arbor.grc.modules.org.web.dto;

import java.util.List;

/** Result of pulling distinct Entra department names into level-1 org units. */
public record DepartmentSyncResponse(
        boolean available,
        String reason,
        int added,
        List<String> names
) {
    public static DepartmentSyncResponse unavailable(String reason) {
        return new DepartmentSyncResponse(false, reason, 0, List.of());
    }

    public static DepartmentSyncResponse ok(int added, List<String> names) {
        return new DepartmentSyncResponse(true, null, added, names);
    }
}
