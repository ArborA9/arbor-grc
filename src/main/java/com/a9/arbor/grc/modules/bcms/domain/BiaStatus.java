package com.a9.arbor.grc.modules.bcms.domain;

public enum BiaStatus {
    DRAFT,
    ASSIGNED,
    IN_PROGRESS,
    SUBMITTED,
    RETURNED,
    /** @deprecated Prefer CLOSED after Head workflow; kept for API compatibility. */
    APPROVED,
    PENDING_BC_HEAD,
    RETURNED_TO_ADMIN,
    READY_TO_CLOSE,
    CLOSED
}
