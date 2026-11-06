package com.a9.arbor.grc.modules.iam.domain;

/**
 * Platform roles. Mapped to Spring Security authorities as {@code ROLE_<name>}.
 *
 * <p>SUPER_ADMIN is cross-tenant (platform operator); the remaining roles are tenant-scoped.
 *
 * <p>{@code INTERNAL_AUDITOR} is the login role labelled Auditor in the UI (Excel Audit MVP v2).
 * Internal Auditor is hidden from Users invite but the enum stays. Data access is time-boxed by
 * an Admin Access Grant. {@code EXTERNAL_AUDITOR} remains for the OTP portal.
 */
public enum Role {
    SUPER_ADMIN,
    OWNER,
    ADMIN,
    CONTRIBUTOR,
    INTERNAL_AUDITOR,
    EXTERNAL_AUDITOR,
    GLOBAL_READER,
    VENDOR
}
