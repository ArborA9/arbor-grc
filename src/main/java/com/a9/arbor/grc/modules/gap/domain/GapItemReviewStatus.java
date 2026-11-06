package com.a9.arbor.grc.modules.gap.domain;

public enum GapItemReviewStatus {
    /** Not yet submitted by Contributor (or Admin drafting unassigned). */
    OPEN,
    /** Contributor submitted: awaiting Admin final status or send-back. */
    SUBMITTED,
    /** Admin sent back for more input; Contributor may re-submit. */
    SENT_BACK,
    /** Admin confirmed final implementation status (Authoritative for scoring). */
    FINALIZED
}
