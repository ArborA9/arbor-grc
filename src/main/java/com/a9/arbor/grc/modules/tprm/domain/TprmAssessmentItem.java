package com.a9.arbor.grc.modules.tprm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tprm_assessment_item")
@Getter
@Setter
public class TprmAssessmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "control_id", nullable = false)
    private Long controlId;

    @Column(name = "control_code", length = 40)
    private String controlCode;

    @Column(length = 80)
    private String domain;

    @Column(length = 2000)
    private String requirement;

    @Column(name = "required_evidence", length = 2000)
    private String requiredEvidence;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "evidence_mandatory")
    private Boolean evidenceMandatory;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_answer", length = 16)
    private TprmAnswer vendorAnswer;

    @Column(length = 4000)
    private String justification;

    @Column(name = "evidence_id")
    private Long evidenceId;

    @Column(name = "evidence_ref", length = 500)
    private String evidenceRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_verdict", length = 40)
    private TprmVerdict reviewVerdict;

    @Column(name = "review_comment", length = 2000)
    private String reviewComment;

    @Column(nullable = false)
    private boolean returned;

    /** Set when Admin returns this control; vendor must change answer/evidence before resubmit. */
    @Column(name = "return_fingerprint", length = 512)
    private String returnFingerprint;
}
