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
@Table(name = "tprm_vendor")
@Getter
@Setter
public class TprmVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_type", length = 64)
    private VendorType vendorType;

    @Column(length = 500)
    private String website;

    @Column(name = "services_in_scope", length = 1000)
    private String servicesInScope;

    @Column(name = "project_description", length = 2000)
    private String projectDescription;

    @Column(name = "org_owner", length = 200)
    private String orgOwner;

    @Column(name = "contract_code", length = 60)
    private String contractCode;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(length = 200)
    private String f1;
    @Column(length = 200)
    private String f2;
    @Column(length = 200)
    private String f3;
    @Column(length = 200)
    private String f4;
    @Column(length = 200)
    private String f5;
    @Column(length = 200)
    private String f6;

    @Column(name = "f1_score")
    private Integer f1Score;
    @Column(name = "f2_score")
    private Integer f2Score;
    @Column(name = "f3_score")
    private Integer f3Score;
    @Column(name = "f4_score")
    private Integer f4Score;
    @Column(name = "f5_score")
    private Integer f5Score;
    @Column(name = "f6_score")
    private Integer f6Score;

    @Column(name = "inherent_score")
    private Integer inherentScore;
    @Column(name = "assurance_credit")
    private Integer assuranceCredit;
    @Column(name = "adjusted_score")
    private Integer adjustedScore;
    @Column(length = 16)
    private String tier;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TprmCriticality criticality;

    @Column(name = "posture_target", precision = 4, scale = 2)
    private BigDecimal postureTarget;

    @Column(name = "reassessment_months")
    private Integer reassessmentMonths;

    @Column(name = "override_tier", length = 16)
    private String overrideTier;

    @Column(name = "override_justification", length = 2000)
    private String overrideJustification;

    @Column(name = "elevation_flag", nullable = false)
    private boolean elevationFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 32)
    private VendorLifecycleState lifecycleState = VendorLifecycleState.PROFILE_INCOMPLETE;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", nullable = false, length = 32)
    private TprmCompliance complianceStatus = TprmCompliance.UNKNOWN;

    @Column(name = "posture_percent", precision = 6, scale = 3)
    private BigDecimal posturePercent;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "review_due_notified_at")
    private Instant reviewDueNotifiedAt;

    @Column(name = "review_overdue_notified_at")
    private Instant reviewOverdueNotifiedAt;

    @Column(name = "contract_expiry_notified_at")
    private Instant contractExpiryNotifiedAt;

    @Column(name = "linked_erm_risk_id")
    private Long linkedErmRiskId;

    @Column(name = "current_assessment_id")
    private Long currentAssessmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VendorStatus status = VendorStatus.ACTIVE;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

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

    public boolean profileComplete() {
        return notBlank(name) && vendorType != null && notBlank(servicesInScope) && notBlank(projectDescription)
                && notBlank(orgOwner) && notBlank(contractCode) && contractStart != null && contractEnd != null
                && notBlank(f1) && notBlank(f2) && notBlank(f3) && notBlank(f4) && notBlank(f5) && notBlank(f6);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
