package com.a9.arbor.grc.modules.gap.domain;

/** How framework/custom questions are scoped when a Gap control is assigned to a Contributor. */
public enum GapQuestionAssignmentMode {
    /** All current (and future) questions on the control: legacy default. */
    ALL,
    /** Only the explicitly selected framework / custom question ids (may be empty). */
    SELECTED
}
