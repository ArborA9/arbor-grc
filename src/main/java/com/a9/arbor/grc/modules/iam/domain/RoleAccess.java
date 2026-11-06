package com.a9.arbor.grc.modules.iam.domain;

import java.util.EnumSet;
import java.util.Set;

/** Shared RBAC helpers for Contributor-only vs elevated tenant roles. */
public final class RoleAccess {

    private static final Set<Role> ELEVATED = EnumSet.of(
            Role.OWNER, Role.ADMIN, Role.GLOBAL_READER, Role.INTERNAL_AUDITOR, Role.EXTERNAL_AUDITOR);

    private RoleAccess() {
    }

    public static boolean isElevated(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(ELEVATED::contains);
    }

    /** True when the user has CONTRIBUTOR and no elevated tenant role. */
    public static boolean isContributorOnly(Set<Role> roles) {
        return roles != null
                && roles.contains(Role.CONTRIBUTOR)
                && !isElevated(roles);
    }

    public static boolean isContributorOnly(User user) {
        return user != null && isContributorOnly(user.getRoles());
    }
}
