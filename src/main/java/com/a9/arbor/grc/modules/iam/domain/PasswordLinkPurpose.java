package com.a9.arbor.grc.modules.iam.domain;

/** Why a set-password link was sent. Drives email wording, expiry, and the screen the link opens. */
public enum PasswordLinkPurpose {
    /** First password for a new account (tenant Owner). */
    ACTIVATION,
    /** User asked to reset, or an admin turned local login back on. */
    RESET
}
