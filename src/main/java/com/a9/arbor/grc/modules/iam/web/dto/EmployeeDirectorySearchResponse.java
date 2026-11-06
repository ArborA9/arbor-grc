package com.a9.arbor.grc.modules.iam.web.dto;

import java.util.List;

/**
 * Entra employee search. {@code available} is true only when Graph returned a payload.
 * Local / non-SSO / Graph-down: {@code available=false}, empty {@code results}, never an error
 * that would block existing screens (this API is unused by the UI today).
 */
public record EmployeeDirectorySearchResponse(
        boolean available,
        String reason,
        List<EmployeeDirectoryEntry> results
) {
    public static EmployeeDirectorySearchResponse unavailable(String reason) {
        return new EmployeeDirectorySearchResponse(false, reason, List.of());
    }

    public static EmployeeDirectorySearchResponse ok(List<EmployeeDirectoryEntry> results) {
        return new EmployeeDirectorySearchResponse(true, null, results == null ? List.of() : List.copyOf(results));
    }
}
