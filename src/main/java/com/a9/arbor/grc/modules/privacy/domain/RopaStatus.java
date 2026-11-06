package com.a9.arbor.grc.modules.privacy.domain;

public enum RopaStatus {
    DRAFT,
    /** Mode B: assigned to Contributor; awaiting workbook + approval evidence. */
    ASSIGNED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED
}
