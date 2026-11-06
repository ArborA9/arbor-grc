package com.a9.arbor.grc.modules.privacy.domain;

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

@Entity
@Table(name = "privacy_ropa")
@Getter
@Setter
public class PrivacyRopa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", nullable = false, length = 32)
    private String displayId;

    @Column(name = "activity_name", nullable = false, length = 255)
    private String activityName;

    @Column(name = "entity_name", length = 255)
    private String entityName;

    @Column(name = "sector_name", length = 255)
    private String sectorName;

    @Column(name = "division_name", length = 255)
    private String divisionName;

    @Column(name = "section_name", length = 255)
    private String sectionName;

    @jakarta.persistence.Lob
    @Column(name = "processing_description")
    private String processingDescription;

    @Column(length = 2000)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_basis", nullable = false, length = 64)
    private RopaLegalBasis legalBasis;

    @Column(name = "data_categories", length = 1000)
    private String dataCategories;

    @Column(name = "sensitive_data_categories", length = 1000)
    private String sensitiveDataCategories;

    @Column(name = "data_subjects", length = 500)
    private String dataSubjects;

    @Column(name = "data_subject_volume", length = 64)
    private String dataSubjectVolume;

    @Column(name = "data_subject_volume_note", length = 500)
    private String dataSubjectVolumeNote;

    @Column(name = "processing_frequency", length = 64)
    private String processingFrequency;

    @Column(name = "processing_frequency_note", length = 500)
    private String processingFrequencyNote;

    @Column(name = "source_of_personal_data", length = 2000)
    private String sourceOfPersonalData;

    @Column(name = "method_of_receiving", length = 2000)
    private String methodOfReceiving;

    @Column(length = 1000)
    private String recipients;

    @Column(name = "share_internal", nullable = false)
    private boolean shareInternal;

    @Column(name = "share_internal_details", length = 2000)
    private String shareInternalDetails;

    @Column(name = "share_operator", nullable = false)
    private boolean shareOperator;

    @Column(name = "share_operator_details", length = 2000)
    private String shareOperatorDetails;

    @Column(name = "share_external", nullable = false)
    private boolean shareExternal;

    @Column(name = "share_external_details", length = 2000)
    private String shareExternalDetails;

    @Column(name = "external_entity_location", length = 500)
    private String externalEntityLocation;

    @Column(name = "third_country_transfers", nullable = false)
    private boolean thirdCountryTransfers;

    @Column(name = "adequate_protection")
    private Boolean adequateProtection;

    @Column(name = "cross_border_transfer_basis", length = 2000)
    private String crossBorderTransferBasis;

    @Column(name = "safeguards_implemented", length = 2000)
    private String safeguardsImplemented;

    @Column(name = "third_country_transfer_details", length = 2000)
    private String thirdCountryTransferDetails;

    @Column(name = "retention_period", length = 255)
    private String retentionPeriod;

    @Column(name = "security_measures", length = 2000)
    private String securityMeasures;

    @Column(name = "storage_systems", length = 2000)
    private String storageSystems;

    @Enumerated(EnumType.STRING)
    @Column(name = "dpia_status", nullable = false, length = 32)
    private RopaDpiaStatus dpiaStatus = RopaDpiaStatus.NOT_REQUIRED;

    /** Legacy boolean kept in sync with {@link #dpiaStatus} == REQUIRED for older clients. */
    @Column(name = "dpia_required", nullable = false)
    private boolean dpiaRequired;

    @Column(name = "linked_dpia_id")
    private Long linkedDpiaId;

    @Column(name = "representative_name", length = 255)
    private String representativeName;

    @Column(name = "representative_employee_id", length = 64)
    private String representativeEmployeeId;

    /** Comment on the Reviewed / Returned review action. */
    @jakarta.persistence.Lob
    private String comments;

    /** Comment on the DPIA required Yes/No decision (kept apart from the review action comment). */
    @jakarta.persistence.Lob
    @Column(name = "dpia_comments")
    private String dpiaComments;

    /** Arbor DPO criteria 1: large volume of Sensitive Personal Data. */
    @Column(name = "dpia_crit_large_sensitive")
    private Boolean dpiaCritLargeSensitive;

    /** Arbor DPO criteria 2: systematic and comprehensive assessment of personal aspects. */
    @Column(name = "dpia_crit_systematic_assessment")
    private Boolean dpiaCritSystematicAssessment;

    /** Arbor DPO criteria 3: automated processing including profiling. */
    @Column(name = "dpia_crit_automated_profiling")
    private Boolean dpiaCritAutomatedProfiling;

    /** Arbor DPO criteria 4: legal consequences or serious impact. */
    @Column(name = "dpia_crit_legal_serious_impact")
    private Boolean dpiaCritLegalSeriousImpact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private RopaDesignation designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_mode", nullable = false, length = 32)
    private RopaIntakeMode intakeMode = RopaIntakeMode.FORM;

    @Column(name = "ropa_department_id")
    private Long ropaDepartmentId;

    /** Champion's work-in-progress: saved at any point, blocked from department submit. */
    @Column(name = "draft")
    private boolean draft;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_outcome", length = 32)
    private RopaReviewOutcome reviewOutcome = RopaReviewOutcome.OPEN;

    @jakarta.persistence.Lob
    @Column(name = "legal_bases_json")
    private String legalBasesJson;

    @Column(name = "adequacy_note", length = 2000)
    private String adequacyNote;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "assigned_contributor_id")
    private Long assignedContributorId;

    @Column(name = "work_task_id")
    private Long workTaskId;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RopaStatus status = RopaStatus.DRAFT;

    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Department Head per-process decision (kept apart from the DPO review outcome). */
    @Enumerated(EnumType.STRING)
    @Column(name = "head_outcome", length = 32)
    private RopaHeadOutcome headOutcome = RopaHeadOutcome.PENDING;

    @jakarta.persistence.Lob
    @Column(name = "head_comments")
    private String headComments;

    @Column(name = "head_decided_by_user_id")
    private Long headDecidedByUserId;

    @Column(name = "head_decided_at")
    private Instant headDecidedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public boolean needsApprovedDpia() {
        return dpiaStatus == RopaDpiaStatus.REQUIRED;
    }

    public void setDpiaStatus(RopaDpiaStatus status) {
        this.dpiaStatus = status != null ? status : RopaDpiaStatus.NOT_REQUIRED;
        this.dpiaRequired = this.dpiaStatus == RopaDpiaStatus.REQUIRED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (dpiaStatus == null) {
            dpiaStatus = RopaDpiaStatus.NOT_REQUIRED;
        }
        dpiaRequired = dpiaStatus == RopaDpiaStatus.REQUIRED;
        if (intakeMode == null) {
            intakeMode = RopaIntakeMode.FORM;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (dpiaStatus != null) {
            dpiaRequired = dpiaStatus == RopaDpiaStatus.REQUIRED;
        }
    }
}
