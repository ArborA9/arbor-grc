package com.a9.arbor.grc.modules.auditmgmt.domain;

public enum AuditType {
    INTERNAL,
    /** Legacy: third-party fieldwork inside Arbor (OTP portal). */
    EXTERNAL,
    /** Certification body findings received and typed in. */
    EXTERNAL_ASSESSMENT,
    FOLLOW_UP
}
