package com.a9.arbor.grc.modules.iam.domain;

/** Account lifecycle state. */
public enum UserStatus {
    /** SSO account created by an admin (pre-invite) but the user has not signed in yet. */
    INVITED,
    ACTIVE,
    DISABLED
}
