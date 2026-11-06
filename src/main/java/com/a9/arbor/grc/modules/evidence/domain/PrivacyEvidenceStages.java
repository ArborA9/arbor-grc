package com.a9.arbor.grc.modules.evidence.domain;

/**
 * Free-form stage strings used when linking evidence to Privacy RoPA Mode B.
 * Not enforced in EvidenceService (unlike RiskEvidenceStage); Privacy service
 * gates submit by requiring both WORKBOOK_SUBMITTED and APPROVAL_EVIDENCE.
 */
public final class PrivacyEvidenceStages {
    public static final String MODULE = "PRIVACY";
    public static final String RECORD_ROPA = "PRIVACY_ROPA";
    public static final String RECORD_DPIA = "PRIVACY_DPIA";
    /** Linked to {@code dpia_instance_question.id} (Champion free-text + attachment answers). */
    public static final String RECORD_DPIA_INSTANCE_QUESTION = "DPIA_INSTANCE_QUESTION";

    public static final String WORKBOOK_WORKING = "WORKBOOK_WORKING";
    public static final String WORKBOOK_SUBMITTED = "WORKBOOK_SUBMITTED";
    public static final String APPROVAL_EVIDENCE = "APPROVAL_EVIDENCE";
    public static final String CHAMPION_ATTACHMENT = "CHAMPION_ATTACHMENT";

    private PrivacyEvidenceStages() {
    }
}
