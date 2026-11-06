package com.a9.arbor.grc.modules.asset.domain;

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
@Table(name = "asset")
@Getter
@Setter
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", length = 32)
    private String displayId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 64)
    private AssetType assetType;

    @Column(name = "asset_sub_type", length = 128)
    private String assetSubType;

    @Column(name = "dependency_text", length = 512)
    private String dependencyText;

    @Column(name = "business_process_service", length = 512)
    private String businessProcessService;

    @Column(length = 64)
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Lob
    private String description;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "owner_job_title", length = 255)
    private String ownerJobTitle;

    @Column(name = "owner_department", length = 255)
    private String ownerDepartment;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "custodian_user_id")
    private Long custodianUserId;

    @Column(name = "custodian_name", length = 255)
    private String custodianName;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(length = 128)
    private String country;

    @Column(name = "site_building", length = 255)
    private String siteBuilding;

    @Column(name = "hosting_model", length = 64)
    private String hostingModel;

    @Column(name = "cloud_provider_region", length = 255)
    private String cloudProviderRegion;

    @Column(name = "data_residency", length = 64)
    private String dataResidency;

    @Column(name = "data_residency_other", length = 255)
    private String dataResidencyOther;

    @Column(length = 255)
    private String hostname;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "asset_tag_serial", length = 128)
    private String assetTagSerial;

    @Column(name = "platform_os_version", length = 255)
    private String platformOsVersion;

    @Column
    private Integer confidentiality;

    @Lob
    @Column(name = "confidentiality_justification")
    private String confidentialityJustification;

    @Column(name = "integrity_rating")
    private Integer integrityRating;

    @Lob
    @Column(name = "integrity_justification")
    private String integrityJustification;

    @Column
    private Integer availability;

    @Lob
    @Column(name = "availability_justification")
    private String availabilityJustification;

    @Column(name = "cia_score", precision = 4, scale = 2)
    private BigDecimal ciaScore;

    @Column(name = "aggregate_score")
    private Integer aggregateScore;

    @Column(name = "weighted_score", precision = 6, scale = 2)
    private BigDecimal weightedScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AssetCriticality criticality;

    /** Final classification (applied override or derived from C). Empty until CIA is rated. */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AssetClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "applied_classification", length = 32)
    private AssetClassification appliedClassification;

    @Column(name = "agreed_rto_hours", precision = 10, scale = 2)
    private BigDecimal agreedRtoHours;

    @Column(name = "agreed_rpo_hours", precision = 10, scale = 2)
    private BigDecimal agreedRpoHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "encrypted_at_rest", length = 32)
    private AssetTriState encryptedAtRest;

    @Enumerated(EnumType.STRING)
    @Column(name = "encrypted_in_transit", length = 32)
    private AssetTriState encryptedInTransit;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_enforced", length = 32)
    private AssetTriState mfaEnforced;

    @Enumerated(EnumType.STRING)
    @Column(name = "backed_up", length = 32)
    private AssetTriState backedUp;

    @Column(name = "backup_frequency", length = 128)
    private String backupFrequency;

    @Column(name = "last_restore_test_date")
    private LocalDate lastRestoreTestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "siem_monitored", length = 32)
    private AssetTriState siemMonitored;

    @Enumerated(EnumType.STRING)
    @Column(name = "vuln_scanned", length = 32)
    private AssetTriState vulnScanned;

    @Column(name = "last_patch_date")
    private LocalDate lastPatchDate;

    @Column(name = "access_review_frequency", length = 128)
    private String accessReviewFrequency;

    @Column(name = "lifecycle_status", length = 64)
    private String lifecycleStatus;

    @Column(name = "acquisition_go_live_date")
    private LocalDate acquisitionGoLiveDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "end_of_life_date")
    private LocalDate endOfLifeDate;

    @Column(name = "disposal_method", length = 512)
    private String disposalMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "third_party_involved", length = 32)
    private AssetTriState thirdPartyInvolved;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @Column(name = "contract_sla_ref", length = 255)
    private String contractSlaRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "tprm_assessment_done", length = 32)
    private AssetTriState tprmAssessmentDone;

    @Column(name = "last_reviewed_date")
    private LocalDate lastReviewedDate;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "change_ticket_ref", length = 255)
    private String changeTicketRef;

    @Lob
    private String notes;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "disposed_at")
    private Instant disposedAt;

    @Column(name = "disposed_by_user_id")
    private Long disposedByUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (lifecycleStatus == null || lifecycleStatus.isBlank()) {
            lifecycleStatus = "Active";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
