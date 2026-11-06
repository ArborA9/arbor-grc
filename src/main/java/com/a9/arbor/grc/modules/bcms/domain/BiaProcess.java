package com.a9.arbor.grc.modules.bcms.domain;

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

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bia_process")
@Getter
@Setter
public class BiaProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_id", nullable = false)
    private Long cycleId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "process_code", nullable = false, length = 64)
    private String processCode;

    @Column(name = "process_name", nullable = false, length = 255)
    private String processName;

    @Column(length = 4000)
    private String description;

    @Column(name = "flow_trigger", length = 4000)
    private String flowTrigger;

    @Column(name = "flow_steps", length = 4000)
    private String flowSteps;

    @Column(name = "flow_output", length = 4000)
    private String flowOutput;

    @Column(name = "third_party_activities", length = 4000)
    private String thirdPartyActivities;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "owner_org_unit_id")
    private Long ownerOrgUnitId;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(length = 64)
    private String frequency;

    @Column(name = "peak_period", length = 255)
    private String peakPeriod;

    @Column(name = "dep_staff", length = 4000)
    private String depStaff;

    @Column(name = "dep_premises", length = 4000)
    private String depPremises;

    @Column(name = "dep_it", length = 4000)
    private String depIt;

    @Column(name = "dep_equipment", length = 4000)
    private String depEquipment;

    @Column(name = "dep_records", length = 4000)
    private String depRecords;

    @Column(name = "dep_third_party", length = 4000)
    private String depThirdParty;

    @Column(name = "related_risk_code", length = 64)
    private String relatedRiskCode;

    @Column(name = "champion_notes", length = 4000)
    private String championNotes;

    @Column(name = "peak_score")
    private Integer peakScore;

    @Column(name = "criticality_tier")
    private Integer criticalityTier;

    @Column(name = "category_label", length = 64)
    private String categoryLabel;

    @Column(name = "rto_hours")
    private Integer rtoHours;

    @Column(name = "mao_hours")
    private Integer maoHours;

    @Column(name = "recovery_priority")
    private Integer recoveryPriority;

    @Column(name = "critical_rank")
    private Integer criticalRank;

    @Column(name = "mor_required", nullable = false)
    private boolean morRequired;

    @Column(name = "rto_caption", length = 500)
    private String rtoCaption;

    @Column(name = "completeness_ok", nullable = false)
    private boolean completenessOk;

    @Column(name = "completeness_label", nullable = false, length = 64)
    private String completenessLabel = "Incomplete";

    @Enumerated(EnumType.STRING)
    @Column(name = "review_outcome", nullable = false, length = 32)
    private BiaReviewOutcome reviewOutcome = BiaReviewOutcome.PENDING_REVIEW;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    @Column(name = "reviewer_name", length = 255)
    private String reviewerName;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "review_comment", length = 4000)
    private String reviewComment;

    /** Set by the Business Continuity Head. Not the Admin specialist outcome. */
    @Column(name = "head_verified", nullable = false)
    private boolean headVerified;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_by_admin", nullable = false)
    private boolean createdByAdmin = true;

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
