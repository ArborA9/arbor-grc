package com.a9.arbor.grc.modules.gap.domain;

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
import java.time.LocalDate;

@Entity
@Table(name = "gap_item")
@Getter
@Setter
public class GapItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gap_assessment_id", nullable = false)
    private Long gapAssessmentId;

    @Column(name = "framework_node_id", nullable = false)
    private Long frameworkNodeId;

    @Column(name = "node_code", nullable = false, length = 64)
    private String nodeCode;

    @Column(nullable = false, length = 512)
    private String title;

    @Lob
    @Column(name = "definition_text")
    private String definitionText;

    /** Catalog "What it means", copied at create and editable by the tenant admin. */
    @Lob
    @Column(name = "what_it_means")
    private String whatItMeans;

    /** Catalog evidence guidance, copied at create and editable by the tenant admin. */
    @Lob
    @Column(name = "evidence_guidance")
    private String evidenceGuidance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GapApplicability applicability;

    @Column(name = "applicability_override", nullable = false)
    private boolean applicabilityOverride;

    @Lob
    @Column(name = "override_justification")
    private String overrideJustification;

    /** Contributor (or Admin drafting) proposed status: not overwritten by Admin final. */
    @Enumerated(EnumType.STRING)
    @Column(name = "implementation_status", length = 32)
    private GapImplementationStatus implementationStatus;

    /** SAMA CSF: contributor maturity 0–5 (target 3). Null for ISO / UAE IA assessments. */
    @Column
    private Integer maturity;

    /**
     * SAMA CSF: Yes/No answers for workbook evidence gates M–Q (JSON {@code {"gates":[…]}}).
     * Drives {@link #maturity} via {@code SamaMaturityGates}.
     */
    @Lob
    @Column(name = "maturity_gates_json")
    private String maturityGatesJson;

    @Lob
    private String observations;

    @Lob
    @Column(name = "gap_description")
    private String gapDescription;

    /** Contributor control-level comments (used when no questions are assigned). */
    @Lob
    @Column(name = "contributor_comments")
    private String contributorComments;

    /** Score for the effective (final if set, else contributor) status. */
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private GapItemReviewStatus reviewStatus = GapItemReviewStatus.OPEN;

    /** Admin's authoritative implementation status: drives assessment score when set. */
    @Enumerated(EnumType.STRING)
    @Column(name = "final_implementation_status", length = 32)
    private GapImplementationStatus finalImplementationStatus;

    /** SAMA CSF: admin-confirmed maturity 0–5. */
    @Column(name = "final_maturity")
    private Integer finalMaturity;

    @Lob
    @Column(name = "admin_review_notes")
    private String adminReviewNotes;

    @Lob
    @Column(name = "send_back_reason")
    private String sendBackReason;

    @Column(name = "contributor_submitted_at")
    private Instant contributorSubmittedAt;

    @Column(name = "admin_reviewed_at")
    private Instant adminReviewedAt;

    @Column(name = "admin_reviewed_by_user_id")
    private Long adminReviewedByUserId;

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @Column(name = "work_task_id")
    private Long workTaskId;

    /**
     * True once the contributor may see this control. A saved decision stays hidden until Send.
     * Rows that already had a task are released.
     */
    @Column(name = "released_to_contributor", nullable = false)
    private boolean releasedToContributor;

    /** Admin mark for later work. Does not change workflow and is cleared when the control is sent. */
    @Column(name = "flagged_for_later", nullable = false)
    private boolean flaggedForLater;

    /** Due date chosen before Send creates the contributor task. */
    @Column(name = "pending_due_date")
    private LocalDate pendingDueDate;

    /**
     * ALL = Contributor gets every question on the control (default / legacy).
     * SELECTED = only {@link #assignedFrameworkQuestionIdsJson} / custom id lists (may be empty).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_assignment_mode", nullable = false, length = 16)
    private GapQuestionAssignmentMode questionAssignmentMode = GapQuestionAssignmentMode.ALL;

    /** JSON array of framework_question ids when mode is SELECTED. */
    @Lob
    @Column(name = "assigned_framework_question_ids_json")
    private String assignedFrameworkQuestionIdsJson;

    /** JSON array of gap_item_custom_question ids when mode is SELECTED. */
    @Lob
    @Column(name = "assigned_custom_question_ids_json")
    private String assignedCustomQuestionIdsJson;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(nullable = false)
    private Instant updatedAt;

    public GapImplementationStatus effectiveImplementationStatus() {
        return finalImplementationStatus != null ? finalImplementationStatus : implementationStatus;
    }

    public Integer effectiveMaturity() {
        return finalMaturity != null ? finalMaturity : maturity;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
