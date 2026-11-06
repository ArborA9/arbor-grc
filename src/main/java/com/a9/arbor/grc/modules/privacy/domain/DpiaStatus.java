package com.a9.arbor.grc.modules.privacy.domain;

public enum DpiaStatus {
    DRAFT,
    /** Opened by Admin marking a ROPA activity DPIA required; DPO fills the form. */
    IN_PROGRESS,
    CANCELLED,
    /** Mode B: assigned to Contributor; awaiting workbook + approval evidence. */
    ASSIGNED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED
}
