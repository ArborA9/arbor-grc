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

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "risk_acceptance_form")
@Getter
@Setter
public class RiskAcceptanceForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_id", nullable = false, unique = true)
    private Long riskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_kind", nullable = false, length = 16)
    private TreatmentFormKind formKind = TreatmentFormKind.ACCEPT;

    @Column(name = "display_id", unique = true, length = 32)
    private String displayId;

    @Column(name = "cycle_no", nullable = false)
    private int cycleNo = 1;

    @Column(name = "approver1_user_id")
    private Long approver1UserId;

    @Column(name = "approver2_user_id")
    private Long approver2UserId;

    @Column(name = "approver3_user_id")
    private Long approver3UserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver2_decision", length = 32)
    private AcceptanceDecision approver2Decision;

    @Lob
    @Column(name = "approver2_comments")
    private String approver2Comments;

    @Column(name = "approver2_reviewed_at")
    private Instant approver2ReviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver3_decision", length = 32)
    private AcceptanceDecision approver3Decision;

    @Lob
    @Column(name = "approver3_comments")
    private String approver3Comments;

    @Column(name = "approver3_reviewed_at")
    private Instant approver3ReviewedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    @Lob
    private String justification;

    @Lob
    @Column(name = "business_rationale")
    private String businessRationale;

    @Lob
    @Column(name = "conditions_of_acceptance")
    private String conditionsOfAcceptance;

    @Lob
    @Column(name = "compensating_controls")
    private String compensatingControls;

    @Column(name = "comp_control_owner_id")
    private Long compControlOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "comp_control_effectiveness", length = 16)
    private RiskLevel compControlEffectiveness;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "contributor_declaration", nullable = false)
    private boolean contributorDeclaration;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_owner_decision", length = 32)
    private AcceptanceDecision riskOwnerDecision;

    @Lob
    @Column(name = "risk_owner_comments")
    private String riskOwnerComments;

    @Column(name = "risk_owner_reviewed_at")
    private Instant riskOwnerReviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_decision", length = 32)
    private AcceptanceDecision adminDecision;

    @Lob
    @Column(name = "admin_comments")
    private String adminComments;

    @Column(name = "admin_reviewed_at")
    private Instant adminReviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AcceptanceFormStatus status = AcceptanceFormStatus.DRAFT;

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
