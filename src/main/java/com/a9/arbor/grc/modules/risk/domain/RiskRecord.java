package com.a9.arbor.grc.modules.risk.domain;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "risk_record")
@Getter
@Setter
public class RiskRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_type", nullable = false, length = 16)
    private RiskType riskType;

    @Column(name = "display_id", nullable = false, unique = true, length = 32)
    private String displayId;

    @Column(nullable = false, length = 1024)
    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private RiskCategory category;

    /** Required when category is OTHER (Doc 03 D3-01). */
    @Column(name = "category_other", length = 128)
    private String categoryOther;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskStatus status = RiskStatus.DRAFT;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "framework_id")
    private Long frameworkId;

    @Column(name = "framework_code", length = 64)
    private String frameworkCode;

    @Column(name = "framework_node_id")
    private Long frameworkNodeId;

    @Column(name = "node_code", length = 64)
    private String nodeCode;

    /** UAE IA P1–P4: IS Risk only; auto from control priority when linked. */
    @Enumerated(EnumType.STRING)
    @Column(name = "uae_ia_priority", length = 8)
    private UaeIaPriority uaeIaPriority;

    @Column(name = "display_id_overridden", nullable = false)
    private boolean displayIdOverridden;

    @Column(name = "auto_close_override", nullable = false)
    private boolean autoCloseOverride;

    @Lob
    @Column(name = "auto_close_override_justification")
    private String autoCloseOverrideJustification;

    @Column(name = "source_dpia_id")
    private Long sourceDpiaId;

    /** Links a Privacy Risk to a DPIA journey DPO assessment row (Risk Title). */
    @Column(name = "source_dpia_assessment_row_id")
    private Long sourceDpiaAssessmentRowId;

    @Column(name = "source_ropa_id")
    private Long sourceRopaId;

    @Column(name = "source_gap_assessment_id")
    private Long sourceGapAssessmentId;

    @Column(name = "source_gap_item_id")
    private Long sourceGapItemId;

    @Column(name = "risk_project_id")
    private Long riskProjectId;

    @Column(name = "date_identified")
    private java.time.LocalDate dateIdentified;

    @Column(length = 128)
    private String cause;

    @Column(name = "inherent_score")
    private Integer inherentScore;

    @Column(name = "residual_score")
    private Integer residualScore;

    @Column(name = "risk_reduction_pct", precision = 5, scale = 2)
    private java.math.BigDecimal riskReductionPct;

    @Column(name = "within_appetite")
    private Boolean withinAppetite;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_decision", length = 32)
    private EvaluationDecision evaluationDecision;

    @Column(name = "acceptance_required", nullable = false)
    private boolean acceptanceRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_control_effectiveness", length = 16)
    private RiskLevel targetControlEffectiveness;

    @Column(name = "target_residual_score")
    private Integer targetResidualScore;

    @Column(name = "target_within_appetite")
    private Boolean targetWithinAppetite;

    @Column(name = "target_residual_manual", nullable = false)
    private boolean targetResidualManual;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_risk_status", length = 16)
    private FinalRiskStatus finalRiskStatus;

    @Lob
    @Column(name = "custodian_comments")
    private String custodianComments;

    @Column(name = "post_treatment_at")
    private Instant postTreatmentAt;

    @Column(name = "post_treatment_by_user_id")
    private Long postTreatmentByUserId;

    @Column(name = "source_compliance_project_id")
    private Long sourceComplianceProjectId;

    @Column(name = "source_risk_template_id")
    private Long sourceRiskTemplateId;

    @Lob
    @Column(name = "observed_gap")
    private String observedGap;

    @Lob
    private String threats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskLevel probability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskLevel impact;

    @Enumerated(EnumType.STRING)
    @Column(name = "inherent_risk", nullable = false, length = 16)
    private RiskLevel inherentRisk;

    @Lob
    @Column(name = "existing_controls")
    private String existingControls;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_effectiveness", nullable = false, length = 16)
    private RiskLevel controlEffectiveness;

    /**
     * Doc 02 R-06: CONTROL_EFFECTIVENESS (default) or MANUAL_PROBABILITY_IMPACT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "residual_scoring_mode", nullable = false, length = 40)
    private ResidualScoringMode residualScoringMode = ResidualScoringMode.MANUAL_PROBABILITY_IMPACT;

    @Enumerated(EnumType.STRING)
    @Column(name = "residual_probability", length = 16)
    private RiskLevel residualProbability;

    @Enumerated(EnumType.STRING)
    @Column(name = "residual_impact", length = 16)
    private RiskLevel residualImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "residual_risk", nullable = false, length = 16)
    private RiskLevel residualRisk;

    /** Matrix grid size (3/4/5) active when this risk was last scored. */
    @Column(name = "matrix_grid_size")
    private Integer matrixGridSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_option", length = 32)
    private TreatmentOption treatmentOption;

    @Lob
    @Column(name = "treatment_plan")
    private String treatmentPlan;

    @Lob
    @Column(name = "treatment_change_justification")
    private String treatmentChangeJustification;

    @Lob
    @Column(name = "elimination_justification")
    private String eliminationJustification;

    @Lob
    @Column(name = "elimination_how")
    private String eliminationHow;

    @Column(name = "activity_stopped")
    private Boolean activityStopped;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_method", length = 32)
    private TransferMethod transferMethod;

    @Column(name = "third_party_name", length = 512)
    private String thirdPartyName;

    @Lob
    @Column(name = "transfer_description")
    private String transferDescription;

    @Lob
    @Column(name = "residual_after_transfer")
    private String residualAfterTransfer;

    @Lob
    @Column(name = "transfer_compensating_controls")
    private String transferCompensatingControls;

    @Lob
    @Column(name = "validation_notes")
    private String validationNotes;

    @Lob
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "validated_by_user_id")
    private Long validatedByUserId;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Column(name = "review_reminder_sent_at")
    private Instant reviewReminderSentAt;

    @Column(name = "risk_owner_user_id")
    private Long riskOwnerUserId;

    @Column(name = "assigned_contributor_id")
    private Long assignedContributorId;

    @Column(name = "work_task_id")
    private Long workTaskId;

    @Lob
    @Column(name = "admin_notes")
    private String adminNotes;

    @Lob
    @Column(name = "infosec_status_initial")
    private String infosecStatusInitial;

    @Lob
    @Column(name = "infosec_status_contributor")
    private String infosecStatusContributor;

    @Lob
    @Column(name = "infosec_status_final")
    private String infosecStatusFinal;

    @Column(name = "assignment_chase_sent_at")
    private Instant assignmentChaseSentAt;

    @Column(name = "owner_chase_sent_at")
    private Instant ownerChaseSentAt;

    @Column(name = "overdue_escalation_sent_at")
    private Instant overdueEscalationSentAt;

    @Column(name = "strategic_objective", length = 512)
    private String strategicObjective;

    @Column(name = "financial_impact_aed", precision = 14, scale = 2)
    private BigDecimal financialImpactAed;

    /** TPRM (req §14.4): set when this ERM risk was raised from a high-risk vendor assessment. */
    @Column(name = "tprm_vendor_id")
    private Long tprmVendorId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
