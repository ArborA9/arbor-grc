package com.a9.arbor.grc.modules.tprm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tprm_assessment")
@Getter
@Setter
public class TprmAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "questionnaire_id")
    private Long questionnaireId;

    @Column(name = "display_id", nullable = false, length = 32)
    private String displayId;

    @Column(length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssessmentStatus status = AssessmentStatus.DRAFT;

    @Column(name = "snapshot_vendor_type", length = 64)
    private String snapshotVendorType;

    @Column(name = "snapshot_tier", length = 16)
    private String snapshotTier;

    @Column(name = "snapshot_criticality", length = 32)
    private String snapshotCriticality;

    @Column(name = "snapshot_target", precision = 4, scale = 2)
    private BigDecimal snapshotTarget;

    @Column(name = "inherent_score")
    private Integer inherentScore;

    @Column(name = "assurance_credit")
    private Integer assuranceCredit;

    @Column(name = "adjusted_score")
    private Integer adjustedScore;

    @Column(name = "posture_percent", precision = 6, scale = 3)
    private BigDecimal posturePercent;

    @Column
    private Boolean compliant;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "raise_erm_prompted", nullable = false)
    private boolean raiseErmPrompted;

    @Column(name = "linked_erm_risk_id")
    private Long linkedErmRiskId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

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
