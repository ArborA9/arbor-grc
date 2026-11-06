package com.a9.arbor.grc.modules.privacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "privacy_dpia")
@Getter
@Setter
public class PrivacyDpia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32)
    private String displayId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "ropa_id")
    private Long ropaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_mode", nullable = false, length = 32)
    private DpiaIntakeMode intakeMode = DpiaIntakeMode.FORM;

    @Column(name = "assigned_contributor_id")
    private Long assignedContributorId;

    @Column(name = "work_task_id")
    private Long workTaskId;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "pre_screening_required", nullable = false)
    private boolean preScreeningRequired;

    @Column(name = "pre_screening_outcome", length = 64)
    private String preScreeningOutcome;

    @Lob
    @Column(name = "pre_screening_notes")
    private String preScreeningNotes;

    @Column(name = "screening_q1", length = 2000)
    private String screeningQ1;

    @Column(name = "screening_q2", length = 2000)
    private String screeningQ2;

    @Column(name = "screening_q3")
    private Boolean screeningQ3;

    @Column(name = "screening_q4")
    private Boolean screeningQ4;

    @Column(name = "screening_q5")
    private Boolean screeningQ5;

    @Column(name = "screening_q6")
    private Boolean screeningQ6;

    @Column(name = "screening_q7")
    private Boolean screeningQ7;

    @Column(name = "full_dpia_mandatory", nullable = false)
    private boolean fullDpiaMandatory;

    @Lob
    @Column(name = "questions_json")
    private String questionsJson;

    @Lob
    @Column(name = "assessment_json")
    private String assessmentJson;

    @Lob
    @Column(name = "overview_risks_json")
    private String overviewRisksJson;

    @Lob
    @Column(name = "processing_description")
    private String processingDescription;

    @Lob
    @Column(name = "necessity_assessment")
    private String necessityAssessment;

    @Lob
    @Column(name = "risks_identified")
    private String risksIdentified;

    @Lob
    @Column(name = "mitigation_measures")
    private String mitigationMeasures;

    @Enumerated(EnumType.STRING)
    @Column(name = "residual_risk", length = 32)
    private DpiaResidualRisk residualRisk;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DpiaStatus status = DpiaStatus.DRAFT;

    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public void recomputeFullDpiaMandatory() {
        fullDpiaMandatory = Boolean.TRUE.equals(screeningQ4)
                || Boolean.TRUE.equals(screeningQ5)
                || Boolean.TRUE.equals(screeningQ6)
                || Boolean.TRUE.equals(screeningQ7);
        if (fullDpiaMandatory) {
            preScreeningRequired = true;
            preScreeningOutcome = "DPIA_REQUIRED";
        } else if (screeningQ4 != null || screeningQ5 != null || screeningQ6 != null || screeningQ7 != null) {
            preScreeningOutcome = "DPIA_NOT_REQUIRED";
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (intakeMode == null) {
            intakeMode = DpiaIntakeMode.FORM;
        }
        recomputeFullDpiaMandatory();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        recomputeFullDpiaMandatory();
    }
}
